    private static final String PROP_DIRECTION = "direction";
    private static final String PROP_EXTENSION = "extension";
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
// REFACTORED: CP-JAVA11

package com.percussion.rx.config.impl;

import com.percussion.design.objectstore.PSConditionalEffect;
import com.percussion.design.objectstore.PSExtensionCall;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.rx.config.IPSConfigHandler.ObjectState;
import com.percussion.rx.config.PSConfigException;
import com.percussion.rx.config.PSConfigValidation;
import com.percussion.rx.design.IPSAssociationSet;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.util.PSCollection;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.*;

/**
 * Setter for the relationship configuration effects.
 * As it is a distributed property and a collection, new effects are added and effects from previous properties are removed.
 *
 * @author bjoginipally
 */
public class PSRelationshipConfigEffectSetter extends PSPropertySetterWithValidation {

    @Override
    protected boolean applyProperty(Object obj, ObjectState state,
                                    List<IPSAssociationSet> aSets, String propName, Object propValue)
            throws Exception {
        if (!(obj instanceof PSRelationshipConfig))
            throw new IllegalArgumentException("obj type must be PSRelationshipConfig.");
        var relConfig = (PSRelationshipConfig) obj;
        if (PROP_EFFECTS.equals(propName)) {
            setEffectsProperty(relConfig, propValue);
        } else {
            super.applyProperty(obj, state, aSets, propName, propValue);
        }
        return true;
    }

    @Override
    protected boolean deApplyProperty(Object obj, List<IPSAssociationSet> aSets, String propName, Object propValue)
            throws Exception {
        if (!(obj instanceof PSRelationshipConfig))
            throw new IllegalArgumentException("obj type must be PSRelationshipConfig.");
        var relConfig = (PSRelationshipConfig) obj;
        if (PROP_EFFECTS.equals(propName)) {
            if (propValue != null)
                removeEffects(relConfig, propValue);
        } else {
            return super.deApplyProperty(obj, aSets, propName, propValue);
        }
        return true;
    }

    @Override
    protected boolean addPropertyDefs(Object obj, String propName, Object pvalue, Map<String, Object> defs) throws PSNotFoundException {
        if (super.addPropertyDefs(obj, propName, pvalue, defs))
            return true;
        if (!(obj instanceof PSRelationshipConfig))
            throw new IllegalArgumentException("obj type must be PSRelationshipConfig.");

        if (PROP_EFFECTS.equals(propName)) {
            addFixmePropertyDefsForList(propName, pvalue, defs);
        }
        return true;
    }

    @Override
    protected Object getPropertyValue(Object obj, String propName) throws PSNotFoundException {
        if (!(obj instanceof PSRelationshipConfig))
            throw new IllegalArgumentException("obj type must be PSRelationshipConfig.");
        var relConfig = (PSRelationshipConfig) obj;
        if (PROP_EFFECTS.equals(propName)) {
            return getEffectProperty(relConfig);
        }
        return super.getPropertyValue(obj, propName);
    }

    @Override
    protected List<PSConfigValidation> validate(String objName, ObjectState state, String propName, Object propValue, Object otherValue) throws PSNotFoundException {
        if (!PROP_EFFECTS.equals(propName))
            return super.validate(objName, state, propName, propValue, otherValue);

        var curEffects = createEffects(propValue);
        var otherEffects = createEffects(otherValue);
        if (curEffects.isEmpty() || otherEffects.isEmpty())
            return Collections.emptyList();
        var curEffNames = getEffectNames(curEffects);
        var othEffNames = getEffectNames(otherEffects);
        curEffNames.retainAll(othEffNames);
        if (curEffNames.isEmpty())
            return Collections.emptyList();

        var msg = " Relationship Type  \"" + objName + "\" has effects \"" +
                curEffNames + "\" that are already configured.";
        var vError = new PSConfigValidation(objName, PROP_EFFECTS, true, msg);
        return Collections.singletonList(vError);
    }

    private List<String> getEffectNames(List<PSConditionalEffect> effects) {
        var effNames = new ArrayList<String>();
        for (var effect : effects) {
            effNames.add(effect.getEffect().getName());
        }
        return effNames;
    }

    private void setEffectsProperty(PSRelationshipConfig relConfig, Object propValue) {
        var prevProps = getPrevProperties();
        Object prevPropValue = null;
        if (prevProps != null && !prevProps.isEmpty()) {
            prevPropValue = prevProps.get("effects");
        }
        mergeEffects(relConfig, propValue, prevPropValue);
    }

    private List<Map<String, Object>> getEffectProperty(PSRelationshipConfig relConfig) {
        var effects = new ArrayList<Map<String, Object>>();
        var iter = relConfig.getEffects();
        while (iter.hasNext()) {
            var effect = (PSConditionalEffect) iter.next();
            effects.add(getEffectPropertyDef(effect));
        }
        return effects;
    }

    private Map<String, Object> getEffectPropertyDef(PSConditionalEffect effect) {
        var efPropDef = new HashMap<String, Object>();
        var execCtxtsList = new ArrayList<String>();
        var execCtxts = effect.getExecutionContexts();
        for (var integer : execCtxts) {
            execCtxtsList.add(PSConditionalEffect.getExecutionContextNameForValue(integer, true));
        }
        efPropDef.put(PROP_EXECUTION_CONTEXT, execCtxtsList);

        var endPoint = effect.getActivationEndPoint();
        var direction = (String) PSConfigUtils.getReverseMap(ms_directionConsts).get(endPoint);
        efPropDef.put(PROP_DIRECTION, direction);

        var extCall = effect.getEffect();
        efPropDef.putAll(PSConfigUtils.getExtensionCallDef(extCall, PROP_EXTENSION));

        var condDef = PSConfigUtils.getCondtionsDef(effect.getConditions());
        efPropDef.put(PROP_CONDITION, condDef);
        return efPropDef;
    }

    private void removeEffects(PSRelationshipConfig relConfig, Object propValue) {
        var iter = relConfig.getEffects();
        var curEffects = new ArrayList<PSConditionalEffect>();
        while (iter.hasNext()) {
            curEffects.add((PSConditionalEffect) iter.next());
        }
        var removals = createEffects(propValue);
        curEffects.removeAll(removals);
        relConfig.setEffects(curEffects.iterator());
    }

    private void mergeEffects(PSRelationshipConfig relConfig, Object propValue, Object prevPropValue) {
        if (propValue == null && prevPropValue == null)
            return;
        var iter = relConfig.getEffects();
        var curEffects = new ArrayList<PSConditionalEffect>();
        while (iter.hasNext()) {
            curEffects.add((PSConditionalEffect) iter.next());
        }

        var newEffects = propValue != null ? createEffects(propValue) : new ArrayList<PSConditionalEffect>();
        var oldEffects = prevPropValue != null ? createEffects(prevPropValue) : new ArrayList<PSConditionalEffect>();

        var removals = new ArrayList<PSConditionalEffect>();
        for (var effect : oldEffects) {
            if (getCorrespondingEffect(effect, newEffects) == null)
                removals.add(effect);
        }

        var mergedEffects = new ArrayList<PSConditionalEffect>();
        for (var effect : curEffects) {
            var mergedEffect = getCorrespondingEffect(effect, newEffects);
            if (mergedEffect == null) {
                mergedEffects.add(effect);
            } else {
                mergedEffects.add(mergedEffect);
                newEffects.remove(mergedEffect);
            }
        }
        mergedEffects.addAll(newEffects);
        mergedEffects.removeAll(removals);

        relConfig.setEffects(mergedEffects.iterator());
    }

    private PSConditionalEffect getCorrespondingEffect(PSConditionalEffect effect, List<PSConditionalEffect> effects) {
        for (var e : effects) {
            if (effect.getEffect().getName().equalsIgnoreCase(e.getEffect().getName())) {
                return e;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<PSConditionalEffect> createEffects(Object propValue) {
        var effects = new ArrayList<PSConditionalEffect>();
        if (propValue == null)
            return effects;
        if (!(propValue instanceof List))
            throw new PSConfigException("The type of the propValue must be \"List\"");
        var tempMaps = (List<Map<String, Object>>) propValue;
        for (var map : tempMaps) {
            var effect = createEffect(map);
            effects.add(effect);
        }
        return effects;
    }

    @SuppressWarnings("unchecked")
    private PSConditionalEffect createEffect(Map<String, Object> map) {
        var extension = (String) map.get(PROP_EXTENSION);
        var direction = (String) map.get(PROP_DIRECTION);
        var execContext = (List<String>) map.get(PROP_EXECUTION_CONTEXT);
        var condition = map.get(PROP_CONDITION);
        if (StringUtils.isBlank(extension)) {
            throw new PSConfigException("The effect is missing required property \"extension\".");
        }
        if (StringUtils.isBlank(direction)) {
            throw new PSConfigException("The effect is missing required property \"direction\".");
        }
        var execCtxts = getExecutionContexts(execContext);
        var extParams = (List<String>) map.get("extensionParams");
        var extCall = PSConfigUtils.createExtensionCall(extension, extParams, "com.percussion.relationship.IPSEffect");
        var endPoint = ms_directionConsts.get(direction.toLowerCase());
        if (endPoint == null) {
            var msg = "The supplied direction ({0}) is invalid.";
            Object[] args = {direction};
            throw new PSConfigException(MessageFormat.format(msg, args));
        }
        var effect = new PSConditionalEffect(extCall);
        effect.setActivationEndPoint(endPoint);
        effect.setExecutionContexts(execCtxts);
        if (condition != null) {
            var conds = PSConfigUtils.prepareConditions(condition);
            effect.setConditions(conds.iterator());
        }
        return effect;
    }

    private List<Integer> getExecutionContexts(List<String> execCtxts) {
        if (execCtxts == null || execCtxts.isEmpty()) {
            throw new PSConfigException("The effect is missing required property \"executionContext\".");
        }
        var ecs = new ArrayList<Integer>();
        for (var ec : execCtxts) {
            var ectx = PSConditionalEffect.getExecutionContextValueForName(ec);
            if (ectx == null) {
                var msg = "The supplied executionContext ({0}) is invalid.";
                Object[] args = {ec};
                throw new PSConfigException(MessageFormat.format(msg, args));
            }
            ecs.add(ectx);
        }
        return ecs;
    }

    private static final String PROP_EFFECTS = "effects";
    private static final Map<String, String> ms_directionConsts = new HashMap<>();

    static {
        ms_directionConsts.put("up", PSRelationshipConfig.ACTIVATION_ENDPOINT_DEPENDENT);
        ms_directionConsts.put("down", PSRelationshipConfig.ACTIVATION_ENDPOINT_OWNER);
        ms_directionConsts.put("either", PSRelationshipConfig.ACTIVATION_ENDPOINT_EITHER);
    }

    private static final String PROP_CONDITION = "condition";
    private static final String PROP_EXECUTION_CONTEXT = "executionContext";
}
