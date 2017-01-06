package org.dspace.rtbf.suggest;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;

public class SuggestHarvester {
	
	private static HarvestScheduler harvester;
	private static Thread mainHarvestThread;
	
	private static Logger log = Logger.getLogger(SuggestHarvester.class);

		
    /**
     * Start harvest scheduler.
     */
    public static synchronized void startNewScheduler() {
        if (mainHarvestThread != null && harvester != null) {
                stopScheduler();
            }
    	harvester = new HarvestScheduler();
    	HarvestScheduler.interrupt = HarvestScheduler.HarvesterInterrupt.NONE;
    	mainHarvestThread = new Thread(harvester);
    	mainHarvestThread.start();
    }

    /**
     * Stop an active harvest scheduler.
     */
    public static synchronized void stopScheduler()  {
        synchronized(HarvestScheduler.lock) {
                HarvestScheduler.interrupt = HarvestScheduler.HarvesterInterrupt.STOP;
                HarvestScheduler.lock.notify();
        }
        mainHarvestThread = null;
                harvester = null;
    }

	
	// Inner classes

	public static class HarvestScheduler implements Runnable {

        public static final Object lock = new Object();
        protected static volatile Integer activeThreads = 0;

        private static enum HarvesterInterrupt {
			NONE(0),
			PAUSE(1),
			STOP(2),
			RESUME(3),
			INSERT_THREAD(4),
			KILL_THREAD(5);
			
			private final int signal;
			
			HarvesterInterrupt(int num) {
				this.signal = num;
			}
			
			public int toInt() {
				return signal;
			}
		}

		private static enum HarvesterStatus {
			NONE(0),
			RUNNING(1),
			SLEEPING(2),
			PAUSED(3),
			STOPPED(4);
			
			private final int status;
			
			HarvesterStatus(int num) {
				this.status = num;
			}
			
			public int toInt() {
				return status;
			}
		}

		private static HarvesterInterrupt interrupt = HarvesterInterrupt.NONE;
		private static HarvesterStatus status = HarvesterStatus.STOPPED;

		@Override
		public void run() {
			scheduleLoop();
		}
		
		private void scheduleLoop() {
			long i = 0;
			while (true) {
				try {
					synchronized (HarvestScheduler.class){
						switch (interrupt) {
						case PAUSE:
							interrupt = HarvesterInterrupt.NONE;
							status = HarvesterStatus.PAUSED;
							break;
						case STOP:
							interrupt = HarvesterInterrupt.NONE;
							status = HarvesterStatus.STOPPED;
							return;
						case NONE:
						default:
							break;
						}
					}

					if (status == HarvesterStatus.PAUSED) {
						while(interrupt != HarvesterInterrupt.RESUME && interrupt != HarvesterInterrupt.STOP) {
							Thread.sleep((1000));
						}
						
						if (interrupt == HarvesterInterrupt.STOP) {
							break; // get out of the while(true)
						}
					}
					// Stage #1: spawn new thread to do the job
					status = HarvesterStatus.RUNNING;
					// 1. is there any new log since last run ?
					String currentTime = DateFormatUtils.format(new Date(), SuggestSearch.DATE_FORMAT_8601);
					String lastSynced = SuggestSearch.getLastSynced();
					if (lastSynced == null) {
						lastSynced = "*";
					}
					
					long newerCount = SolrLogger.queryTotal("*:*", "time:[" +lastSynced+ " TO " +currentTime+ "]").getCount();

					// 2. start a new thread
					if (newerCount > 0 && activeThreads == 0) {
						synchronized(HarvestScheduler.class){
							activeThreads++;
						}
						Thread activeThread = new Thread(new HarvestThread(lastSynced, currentTime));
						activeThread.start();

                        // 3. wait the thread to finish
                        while (activeThreads != 0) {
                        	/* Wait a second */
                        	Thread.sleep(1000);
                        }
                        
                        // log.info("Done with iteration " + i++);
					}
				} catch (Exception e) {
                    log.error("Something get wrong. Aborting.");
                 }
				
				
				// Stage #2: schedule next run
                status = HarvesterStatus.SLEEPING;
                try {
                	synchronized(lock) {
                		/* Start over again in 60 sec */
                		lock.wait(60000);
                	}
                } catch (InterruptedException ie) {
                    log.warn("Interrupt: " + ie.getMessage());
                }

			}
			
			i++;
			
		}
		
	}
	
    private static class HarvestThread extends Thread {

        public static final int      DEFAULT_RPP = 100;
        public static final int      DEFAULT_OFFSET = 0;

        String lastSynced;
		String currentTime;

        public HarvestThread(String lastSynced, String currentTime) {
        	this.lastSynced = lastSynced;
			this.currentTime = currentTime;
		}

        @Override
		public void run() {
            runHarvest();
		}
        
        private void runHarvest() {
			try {
				long newerCount = SolrLogger.queryTotal("*:*", "time:[" +this.lastSynced+ " TO " +this.currentTime+ "]").getCount();
				log.info("New harvest thread : lastTime=" + lastSynced + " newerCount=" + String.valueOf(newerCount));		
				
				for (int i = 0; i < newerCount; i += DEFAULT_RPP) {
					// TODO pass offset parameter
					ObjectCount[] newerQueries = SolrLogger.queryNewerThen("*:*", "time:[" +this.lastSynced+ " TO " +this.currentTime+ "]"
							,"query_q", i /* offset */, DEFAULT_RPP);
					for (ObjectCount newerQuery : newerQueries) {
						SuggestSearch.merge(newerQuery.getValue(), newerQuery.getCount());
					}
				}
				
				SuggestSearch.updLastSynced(currentTime);
				
			} catch (SolrServerException | IOException e) {
				log.error(" Harvest thread aborting: " + e.getMessage());
			} finally {
	        	synchronized (HarvestScheduler.class) {
	        		HarvestScheduler.activeThreads--;
	        	}
			}
        }


    }
     


	
}
