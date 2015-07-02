/*
 * Copyright 2005-2010 LAMP/EPFL
 */
// $Id$

package argus.tools.eclipse.contribution.weaving.jdt.launching;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.launching.JavaLaunchableTester;

import argus.tools.eclipse.contribution.weaving.jdt.IJawaElement;

@SuppressWarnings("restriction")
public privileged aspect JavaLaunchableTesterAspect {
  pointcut launchTesters(IJavaElement element, String name) :
    execution(boolean JavaLaunchableTester.hasSuperclass(IJavaElement, String)) && args(element, name);

  pointcut hasMain(IJavaElement element) :
    execution(boolean JavaLaunchableTester.hasMain(IJavaElement)) && args(element);

  boolean around(IJavaElement element) :
    hasMain(element) {
    if (element instanceof IJawaElement) {
      return false;
    } else
      return proceed(element);
  }

  boolean around(IJavaElement element, String name) :
    launchTesters(element, name) {
    if (element instanceof IJawaElement) {
      return false;
    } else
      return proceed(element, name);
  }
}
