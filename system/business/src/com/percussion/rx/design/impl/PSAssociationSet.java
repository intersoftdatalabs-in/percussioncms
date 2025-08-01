/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
// REFACTORED: CP-JAVA11
package com.percussion.rx.design.impl;

import com.percussion.rx.design.IPSAssociationSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Java 11 refactored implementation of IPSAssociationSet.
 * Maintains a set of associations for a design object.
 */
public class PSAssociationSet implements IPSAssociationSet {

    private List<Object> associations = new ArrayList<>();
    private final AssociationType associationType;
    private AssociationAction action = AssociationAction.REPLACE;

    /**
     * Constructs an association set.
     *
     * @param associationType the association type, must not be null
     */
    public PSAssociationSet(AssociationType associationType) {
        this.associationType = Objects.requireNonNull(associationType, "associationType must not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AssociationType getType() {
        return associationType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Object> getAssociations() {
        return Collections.unmodifiableList(associations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void setAssociations(List associations) {
        if (associations == null) {
            this.associations = new ArrayList<>();
        } else {
            this.associations = new ArrayList<>();
            for (Object obj : associations) {
                this.associations.add(obj);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AssociationAction getAction() {
        return action;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAction(AssociationAction action) {
        this.action = Objects.requireNonNull(action, "Association Action may not be null.");
    }
}
