package org.arguside.core.internal.launching

import org.eclipse.core.resources.IProject
import org.eclipse.jdt.launching.JavaLaunchDelegate

class ArgusLaunchDelegate extends JavaLaunchDelegate with ClasspathGetterForLaunchDelegate
    with ProblemHandlersForLaunchDelegate with MainClassFinalCheckForLaunchDelegate {
  override val existsProblemsAccessor: IProject => Boolean = existsProblems _
}
