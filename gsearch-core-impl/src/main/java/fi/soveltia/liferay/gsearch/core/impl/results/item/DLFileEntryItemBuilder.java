
package fi.soveltia.liferay.gsearch.core.impl.results.item;

import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.document.library.kernel.util.DLUtil;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalService;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalServiceUtil;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringBundler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import fi.soveltia.liferay.gsearch.core.api.constants.GSearchWebKeys;

/**
 * DLFileEntry item type result builder.
 * 
 * @author Petteri Karttunen
 */
@Component(
	immediate = true
)
public class DLFileEntryItemBuilder extends BaseResultItemBuilder {

	/**
	 * {@inheritDoc}
	 * @throws Exception 
	 */
	@Override
	public String getImageSrc() throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)_portletRequest.getAttribute(GSearchWebKeys.THEME_DISPLAY);
		
		FileEntry fileEntry = _dLAppService.getFileEntry(_entryClassPK);
		
		return DLUtil.getThumbnailSrc(fileEntry, themeDisplay);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getLink() {

		StringBundler sb = new StringBundler();
		sb.append(PortalUtil.getPortalURL(_portletRequest));
		sb.append("/documents/");
		sb.append(_document.get(Field.SCOPE_GROUP_ID));
		sb.append("/");
		sb.append(_document.get(Field.FOLDER_ID));
		sb.append("/");
		sb.append(_document.get("path"));

		return sb.toString();
	}
	
	/**
	 * {@inheritDoc}
	 * @throws Exception 
	 */
	@Override
	public Map<String, String> getMetadata() throws Exception {

		Map<String, String>metaData = new HashMap<String, String>();

		String mimeType =  _document.get("mimeType");
		
		// Format
		metaData.put("format", translateMimetype(mimeType));
		
		// Size

		metaData.put("size", getSize());

		// Image metadata
		
		if (mimeType.startsWith("image_")) {
			setImageMetadata(metaData);
		}

		return metaData;
	}

	/**
	 * Set image metadata.
	 * 
	 * @param metaData
	 * @throws Exception
	 */
	protected  void setImageMetadata(Map<String, String> metaData) throws Exception {
				
		 // Dimensions
		
		 StringBundler sb = new StringBundler();
		 sb.append(_document.get(getTikaRawMetadataField("WIDTH")));
		 sb.append(" x ");
		 sb.append(_document.get(getTikaRawMetadataField("LENGTH")));
		 sb.append(" px");
		
		 metaData.put("dimensions", sb.toString());		 
	}

	/**
	 * Translate mimetype for UI
	 * 
	 * @param mimeType
	 * @return
	 */
	protected String translateMimetype(String mimeType) {

		if (mimeType.equals(MIMETYPE_WORD)) {
			return "DOCX";
		} else if (mimeType.equals(MIMETYPE_EXCEL)) {
			return "XLSX";
		} else if (mimeType.equals(MIMETYPE_POWERPOINT)) {
			return "PPTX";
		} else if (mimeType.startsWith("image_")) {
			return mimeType.split("image_")[1];
		} else if (mimeType.startsWith("application_")) {
			return mimeType.split("application_")[1];
		}
		
		return mimeType;
	}

	/**
	 * Beautify file size
	 * 
	 * @param size
	 * @param locale
	 * @return
	 */
	protected String getSize() {
		
		long size = Long.valueOf(_document.get("size"));
		
		StringBundler sb = new StringBundler();
		
		if (size >= MBYTES) {
			sb.append(Math.round(size / (float) MBYTES)).append(" MB");

		} else if (size >= KBYTES) {
			sb.append(Math.round(size / (float) KBYTES)).append(" KB");
		} else { 
			sb.append(1).append(" KB");
		}
		return sb.toString();
	}	
	
	/**
	 * Get index translated field name for a Tikaraw metadata field.
	 * 
	 * @param key
	 * @return
	 * @throws Exception
	 */
	protected String getTikaRawMetadataField(String key) throws Exception {

		StringBundler sb = new StringBundler();
		sb.append("ddm__text__");
		sb.append(String.valueOf(getTikaRawStructureId()));
		sb.append("__TIFF_IMAGE_");
		sb.append(key);
		sb.append("_");
		sb.append(_locale.toString());
		sb.append("_sortable");
		
		return sb.toString();
	}
	
	/**
	 * Get the id for structure holding image metadata ("TIKARAWMETADATA")
	 * 
	 * @return
	 * @throws Exception
	 */
	protected long getTikaRawStructureId() throws Exception{
		
		if (TIKARAW_STRUCTURE_ID == null) {

			DynamicQuery structureQuery = _ddmStructureLocalService.dynamicQuery();
			structureQuery.add(
				RestrictionsFactoryUtil.eq("structureKey", "TIKARAWMETADATA"));

			List<DDMStructure> structures =
							DDMStructureLocalServiceUtil.dynamicQuery(structureQuery);
						
			DDMStructure structure =  structures.get(0);
			
			TIKARAW_STRUCTURE_ID = structure.getStructureId();
		}
		return TIKARAW_STRUCTURE_ID;
	}
	
	protected final static String MIMETYPE_WORD = "application_vnd.openxmlformats-officedocument.wordprocessingml.document";
	protected final static String MIMETYPE_EXCEL = "application_vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	protected final static String MIMETYPE_POWERPOINT = "application_vnd.openxmlformats-officedocument.presentationml.presentation";

	protected static final long KBYTES = 1024;
	protected static final long MBYTES = 1024 * 1024;
	
	protected static Long TIKARAW_STRUCTURE_ID = null;

	@Reference(unbind = "-")
	protected void setDDMStructureLocalService(
		DDMStructureLocalService ddmStructureLocalService) {

		_ddmStructureLocalService = ddmStructureLocalService;
	}
	
	@Reference(unbind = "-")
	protected void setDLAppService(
		DLAppService dLAppService) {

		_dLAppService = dLAppService;
	}	

	@Reference
	protected static DDMStructureLocalService _ddmStructureLocalService;
	
	@Reference
	protected static DLAppService _dLAppService;
}