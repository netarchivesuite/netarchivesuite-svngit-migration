package dk.netarkivet.harvester.webinterface;

import java.util.Iterator;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendableEntity;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedField;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDBDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDataTypes;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDefaultValue;


public class ExtendedFieldValueDefinition {
    private static Log log = LogFactory.getLog(ExtendedFieldValueDefinition.class.getName());

    /*
     * Subprocessing of ServletRequest for Extended Fields and update field content
     * @param context The context of this request
     * @param i18n I18n information
     * @param entity ExtendableEntity 
     * @param type ExtendedFieldType 
     */
    public static void processRequest(PageContext context, I18n i18n, ExtendableEntity entity, int type) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");
    	
    	
        ExtendedFieldDAO extdao = ExtendedFieldDBDAO.getInstance();
        Iterator<ExtendedField> it = extdao.getAll(type).iterator();
        
        ServletRequest request = context.getRequest();
        
        while (it.hasNext()) {
            String value = "";

            ExtendedField ef = it.next();
            String parameterName = ef.getJspFieldname();
            switch (ef.getDatatype()) {
            case ExtendedFieldDataTypes.BOOLEAN:
                String[] parb = request.getParameterValues(parameterName);
                if (parb != null && parb.length > 0) {
                    value = ExtendedFieldConstants.TRUE;
                } else {
                    value = ExtendedFieldConstants.FALSE;
                }
                break;
            case ExtendedFieldDataTypes.SELECT:
                String[] pars = request.getParameterValues(parameterName);
                if (pars != null && pars.length > 0) {
                    value = pars[0];
                } else {
                    value = "";
                }

                break;
            default:
                value = request.getParameter(parameterName);
                if (ef.isMandatory()) {
                    if (value == null || value.length() == 0) {
                        value = ef.getDefaultValue();
                    }

                    if (value == null || value.length() == 0) {
                        HTMLUtils.forwardWithErrorMessage(
                                context,
                                i18n,
                                "errormsg;extendedfields.field.0.is.empty."
                                + "but.mandatory",
                                ef.getName());
                        throw new ForwardedToErrorPage("Mandatory field "
                                + ef.getName() + " is empty.");
                    }
                }

                ExtendedFieldDefaultValue def = new ExtendedFieldDefaultValue(value, ef.getFormattingPattern(), ef.getDatatype());
                if (!def.isValid()) {
                    HTMLUtils.forwardWithRawErrorMessage(context, i18n, "errormsg;extendedfields.value.invalid");
                    throw new ForwardedToErrorPage("errormsg;extendedfields.value.invalid");
                }
                value = def.getDBValue();
                break;
            }

            entity.updateExtendedFieldValue(ef.getExtendedFieldID(), value);
        }
    }
    

}
