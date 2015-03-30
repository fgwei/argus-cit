package amanide.editors.codecompletion;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This class contains all the information we need from the folders beneath the
 * pilarpath.
 * 
 * @author Fengguo Wei
 */
public class ModulesFoundStructure {

	/**
	 * Contains: file found -> module name it was found with.
	 */
	public Map<File, String> regularModules = new HashMap<File, String>();
}
