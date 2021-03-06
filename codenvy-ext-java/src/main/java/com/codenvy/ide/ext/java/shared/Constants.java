/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.ide.ext.java.shared;

/** @author Artem Zatsarynnyy */
public interface Constants {
    final String MAVEN_ID = "maven";
    final String ANT_ID   = "ant";

    // project type names
    final String MAVEN_NAME                   = "Maven Project";
    final String ANT_NAME                     = "Ant Project";
    // project categories
    final String JAVA_CATEGORY                = "Java";
    // project attribute names
    final String LANGUAGE                     = "language";
    final String LANGUAGE_VERSION             = "language.version";
    final String FRAMEWORK                    = "framework";
    final String FRAMEWORK_VERSION            = "framework.version";
    final String BUILDER_ANT_SOURCE_FOLDERS   = "builder.ant.source_folders";
    final String BUILDER_MAVEN_SOURCE_FOLDERS = "builder.maven.source_folders";
}
