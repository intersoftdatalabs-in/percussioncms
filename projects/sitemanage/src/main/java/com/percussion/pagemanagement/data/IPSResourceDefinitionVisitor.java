// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.pagemanagement.data;

import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSAssetResource;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSFileResource;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSFolderResource;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSResourceDefinition;
import com.percussion.share.service.exception.PSDataServiceException;

/**
 * Visitor pattern for the different types of {@link PSResourceDefinition}s.
 * Implementations should handle each resource type accordingly.
 * @author adamgent
 */
public interface IPSResourceDefinitionVisitor {

    /**
     * Visit an asset resource.
     * @param resource the asset resource, never {@code null}
     * @throws PSDataServiceException if a data service error occurs
     */
    void visit(PSAssetResource resource) throws PSDataServiceException;

    /**
     * Visit a file resource.
     * @param resource the file resource, never {@code null}
     */
    void visit(PSFileResource resource);

    /**
     * Visit a folder resource.
     * @param resource the folder resource, never {@code null}
     */
    void visit(PSFolderResource resource);

    /**
     * Visit a theme resource.
     * @param resource the theme resource, never {@code null}
     */
    void visit(PSThemeResource resource);
}
