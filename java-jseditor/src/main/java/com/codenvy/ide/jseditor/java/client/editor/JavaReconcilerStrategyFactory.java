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
package com.codenvy.ide.jseditor.java.client.editor;

import com.codenvy.ide.api.texteditor.outline.OutlineModel;
import com.codenvy.ide.jseditor.client.annotation.AnnotationModel;
import com.codenvy.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;

/**
 * Factory of reconciler factories for java documents.
 */
public interface JavaReconcilerStrategyFactory {
    JavaReconcilerStrategy create(EmbeddedTextEditorPresenter editor,
                                  OutlineModel outlineModel,
                                  JavaCodeAssistProcessor codeAssistProcessor,
                                  AnnotationModel annotationModel);
}
