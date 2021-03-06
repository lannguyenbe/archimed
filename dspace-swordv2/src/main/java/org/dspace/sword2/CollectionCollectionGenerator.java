/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.apache.abdera.i18n.iri.IRI;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.swordapp.server.SwordCollection;

import java.util.Map;
import java.util.List;

/**
 * Class to generate ATOM Collection Elements which represent
 * DSpace Collections
 *
 */
public class CollectionCollectionGenerator implements AtomCollectionGenerator
{
	private static Logger log = Logger.getLogger(CommunityCollectionGenerator.class);

	/**
	 * Build the collection for the given DSpaceObject.  In this implementation,
	 * if the object is not a DSpace COllection, it will throw an exception
	 * @param dso
	 * @return
	 * @throws DSpaceSwordException
	 */
	public SwordCollection buildCollection(Context context, DSpaceObject dso, SwordConfigurationDSpace swordConfig)
			throws DSpaceSwordException
	{
		if (!(dso instanceof org.dspace.content.Collection))
		{
			log.error("buildCollection passed argument which is not of type Collection");
			throw new DSpaceSwordException("Incorrect ATOMCollectionGenerator instantiated");
		}

		// get the things we need out of the service
		SwordUrlManager urlManager = swordConfig.getUrlManager(context, swordConfig);

		Collection col = (Collection) dso;
		SwordCollection scol = new SwordCollection();

		// prepare the parameters to be put in the sword collection
		String location = urlManager.getDepositLocation(col);

		// collection title is just its name
		String title = col.getMetadata("name");

		// the collection policy is the licence to which the collection adheres
		String collectionPolicy = col.getLicense();

		// FIXME: what is the treatment?  Doesn't seem appropriate for DSpace
		// String treatment = " ";

		// abstract is the short description of the collection
		String dcAbstract = col.getMetadata("short_description");

		// we just do support mediation
		boolean mediation = swordConfig.isMediated();

		// load up the sword collection
		scol.setLocation(location);

		// add the title if it exists
		if (title != null && !"".equals(title))
		{
			scol.setTitle(title);
		}

		// add the collection policy if it exists
		if (collectionPolicy != null && !"".equals(collectionPolicy))
		{
			scol.setCollectionPolicy(collectionPolicy);
		}

		// FIXME: leave the treatment out for the time being,
		// as there is no analogue
		// scol.setTreatment(treatment);

		// add the abstract if it exists
		if (dcAbstract != null && !"".equals(dcAbstract))
		{
			scol.setAbstract(dcAbstract);
		}

		scol.setMediation(mediation);

        List<String> accepts = swordConfig.getCollectionAccepts();
        for (String accept : accepts)
        {
            scol.addAccepts(accept);
			scol.addMultipartAccepts(accept);
        }

		// add the accept packaging values
		List<String> aps = swordConfig.getAcceptPackaging(col);
		for (String ap : aps)
		{
			scol.addAcceptPackaging(ap);
		}

		// should we offer the items in the collection up as deposit
		// targets?
		boolean itemService = ConfigurationManager.getBooleanProperty("sword.expose-items");
		if (itemService)
		{
			String subService = urlManager.constructSubServiceUrl(col);
			scol.addSubService(new IRI(subService));
		}

		log.debug("Created ATOM Collection for DSpace Collection");

		return scol;
	}
}
