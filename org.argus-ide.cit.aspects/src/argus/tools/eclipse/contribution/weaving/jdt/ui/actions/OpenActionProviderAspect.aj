package argus.tools.eclipse.contribution.weaving.jdt.ui.actions;

import java.util.List;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.actions.OpenAction;

import argus.tools.eclipse.contribution.weaving.jdt.ArgusJDTWeavingPlugin;
import argus.tools.eclipse.contribution.weaving.jdt.ui.javaeditor.IPilarEditor;

/**
 * When the user right clicks on a element and select "Open Declaration" in the
 * context menu, an instance of <code>OpenAction</code> is used to resolve the
 * binding and jump to the declaration. Because the Eclipse API does not expose
 * an extension point for this action, we need to create a custom one and
 * intercept the creation of an <code>OpenAction</code>, if it originates from a
 * <code>IPilarEditor</code>.
 */
@SuppressWarnings("restriction")
public privileged aspect OpenActionProviderAspect {

  pointcut newInstance(JavaEditor editor): 
		call(public OpenAction.new(JavaEditor)) && 
		args(editor);

  OpenAction around(JavaEditor editor) : newInstance(editor) {
    if (editor instanceof IPilarEditor) {
      List<IOpenActionProvider> providers = OpenActionProviderRegistry
          .getInstance().getProviders();
      if (providers.size() == 1) {
        IOpenActionProvider provider = providers.get(0);
        return provider.getOpenAction(editor);
      } else if (providers.isEmpty()) {
        return proceed(editor);
      } else {
        String msg = "Found multiple provider classes for extension point `"
            + OpenActionProviderRegistry.OPEN_ACTION_PROVIDERS_EXTENSION_POINT
            + "`.\n"
            + "This is ambiguos, therefore I'm going to ignore this custom extension point and use the default implementation.\n"
            + "\tHint: To fix this look in your plugin.xml file and make sure to declare at most one provider class for the extension point: "
            + OpenActionProviderRegistry.OPEN_ACTION_PROVIDERS_EXTENSION_POINT;

        ArgusJDTWeavingPlugin.logErrorMessage(msg);

        return proceed(editor);
      }
    } else {
      return proceed(editor);
    }
  }
}
