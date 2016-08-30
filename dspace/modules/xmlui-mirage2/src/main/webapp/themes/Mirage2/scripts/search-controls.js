/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 *
 * Lan 03.04.2015 : The Search box feature in handlebars has been removed from here 
 * because ported to Angular
 */
(function($){

        $(function () {
            assignGlobalEventHandlers();
        });




    function assignGlobalEventHandlers() {
        $('.show-advanced-filters').click(function () {
            var wrapper = $('#aspect_discovery_SimpleSearch_div_discovery-filters-wrapper');
            wrapper.parent().find('.discovery-filters-wrapper-head').hide().removeClass('hidden').fadeIn(200);
            wrapper.hide().removeClass('hidden').slideDown(200);
            $(this).addClass('hidden');
            $('.hide-advanced-filters').removeClass('hidden');
            return false;
        });

        $('.hide-advanced-filters').click(function () {
            var wrapper = $('#aspect_discovery_SimpleSearch_div_discovery-filters-wrapper');
            wrapper.parent().find('.discovery-filters-wrapper-head').fadeOut(200, function() {
                $(this).addClass('hidden').removeAttr('style');
            });
            wrapper.slideUp(200, function() {
                $(this).addClass('hidden').removeAttr('style');
            });
            $(this).addClass('hidden');
            $('.show-advanced-filters').removeClass('hidden');
            return false;
        });


        $('.controls-gear-wrapper').find('li.gear-option,li.gear-option a').click(function(event){
            var value, param, mainForm, params, listItem, $this;
            event.stopPropagation();
            $this = $(this);
            if($this.is('li')){
                listItem = $this;
            }else{
                listItem = $this.parents('li:first');
            }

            //Check if this option is currently selected, if so skip the next stuff
            if(listItem.hasClass('gear-option-selected')){
                return false;
            }
            if(!$this.attr('href')){
                $this = $this.find('a');
            }
            //Retrieve the params we are to fill in in our main form
            params = $this.attr('href').split('&');

            mainForm = $('#aspect_discovery_SimpleSearch_div_main-form');
            //Split them & fill in in the main form, when done submit the main form !
            for(var i = 0; i < params.length; i++){
                param = params[i].split('=')[0];
                value = params[i].split('=')[1];

                mainForm.find('input[name="' + param + '"]').val(value);
            }
            //Clear the page param
            mainForm.find('input[name="page"]').val('1');

            mainForm.submit();
            $this.closest('.open').removeClass('open');
            return false;
        });
    }

})(jQuery);
