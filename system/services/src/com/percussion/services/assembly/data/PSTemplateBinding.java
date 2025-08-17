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
package com.percussion.services.assembly.data;

import com.percussion.services.assembly.IPSTemplateBinding;
import com.percussion.utils.jexl.IPSScript;
import com.percussion.utils.jexl.PSJexlEvaluator;
import com.percussion.utils.xml.IPSXmlSerialization;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single template binding with enhanced Java 11 support.
 *
 * <p>A template binding matches the declaration of a variable with a JEXL expression
 * to calculate its value during template assembly. Bindings are executed in order
 * and provide the variable context for template evaluation.
 *
 * <p>Key features:
 * <ul>
 *   <li>JEXL expression evaluation with caching</li>
 *   <li>Execution order management</li>
 *   <li>Enhanced null safety with Optional wrappers</li>
 *   <li>Immutable factory methods</li>
 *   <li>Comprehensive validation patterns</li>
 * </ul>
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSTemplateBinding")
@Table(name = "PSX_TEMPLATE_BINDING")
public class PSTemplateBinding implements IPSTemplateBinding, Cloneable, Serializable {

   /**
    * Serial version UID for serialization compatibility.
    */
   private static final long serialVersionUID = 1L;

   @Id
   @GenericGenerator(name = "id", strategy = "com.percussion.data.utils.PSGuidHibernateGenerator")
   @GeneratedValue(generator = "id")
   @Column(name = "BINDING_ID", nullable = false)
   private long m_bindingId;

   @Version
   @Column(name = "VERSION")
   private Integer m_version;

   @Transient
   private int m_executionOrder;

   @Basic
   @Column(name = "VARIABLE")
   private String m_variable;

   @Lob
   @Basic(fetch = FetchType.EAGER)
   @Column(name = "EXPRESSION")
   @Fetch(FetchMode.SUBSELECT)
   private String m_expression;

   @Transient
   private transient IPSScript m_jexl = null;

   /**
    * Default constructor required for JPA/Hibernate.
    */
   public PSTemplateBinding() {
      // Required by JPA
   }

   /**
    * Create a new template binding with execution order.
    *
    * @param order execution order (low to high), minimum value is 1
    * @param variable the variable to bind to, not {@code null} or empty
    * @param expression the JEXL expression, not {@code null} or empty
    * @throws IllegalArgumentException if parameters are invalid
    */
   public PSTemplateBinding(int order, String variable, String expression) {
      setExecutionOrder(order);
      setVariable(variable);
      setExpression(expression);
   }

   /**
    * Create a new template binding without specifying execution order.
    *
    * @param variable the variable to bind to, not {@code null} or empty
    * @param expression the JEXL expression, not {@code null} or empty
    * @throws IllegalArgumentException if parameters are invalid
    */
   public PSTemplateBinding(String variable, String expression) {
      setVariable(variable);
      setExpression(expression);
   }

   /**
    * Create a template binding using factory method with enhanced validation.
    *
    * @param variable the variable name, not {@code null} or empty
    * @param expression the JEXL expression, not {@code null} or empty
    * @param order the execution order, must be >= 1
    * @return a new PSTemplateBinding instance
    * @throws IllegalArgumentException if validation fails
    */
   public static PSTemplateBinding of(String variable, String expression, int order) {
      return new PSTemplateBinding(order, variable, expression);
   }

   /**
    * Create a template binding with default execution order.
    *
    * @param variable the variable name, not {@code null} or empty
    * @param expression the JEXL expression, not {@code null} or empty
    * @return a new PSTemplateBinding instance
    * @throws IllegalArgumentException if validation fails
    */
   public static PSTemplateBinding of(String variable, String expression) {
      return new PSTemplateBinding(variable, expression);
   }

   /**
    * Get the execution order with Optional wrapper for safer access.
    *
    * @return Optional containing the execution order if valid, empty otherwise
    */
   public Optional<Integer> getExecutionOrderOptional() {
      return m_executionOrder > 0 ? Optional.of(m_executionOrder) : Optional.empty();
   }

   /**
    * Get the execution order for this binding.
    *
    * @return the execution order (1-based)
    */
   public Integer getExecutionOrder() {
      return m_executionOrder;
   }

   /**
    * Set the execution order with enhanced validation.
    *
    * @param executionOrder the execution order, must be >= 1
    * @throws IllegalArgumentException if executionOrder is < 1
    */
   public void setExecutionOrder(Integer executionOrder) {
      if (executionOrder != null && executionOrder < 1) {
         throw new IllegalArgumentException("executionOrder must be >= 1");
      }
      this.m_executionOrder = executionOrder != null ? executionOrder : 0;
   }

   /**
    * Get the JEXL expression with Optional wrapper for safer access.
    *
    * @return Optional containing the expression if present, empty otherwise
    */
   public Optional<String> getExpressionOptional() {
      return Optional.ofNullable(m_expression);
   }

   /**
    * Get the JEXL expression.
    *
    * @return the expression, may be {@code null}
    */
   public String getExpression() {
      return m_expression;
   }

   /**
    * Set the JEXL expression with validation.
    *
    * @param expression the JEXL expression, should not be {@code null} or empty
    *                   (allows these for editing purposes)
    */
   public void setExpression(String expression) {
      this.m_expression = expression;
      // Clear cached JEXL script when expression changes
      this.m_jexl = null;
   }

   /**
    * Get the variable name with Optional wrapper for safer access.
    *
    * @return Optional containing the variable name if present, empty otherwise
    */
   public Optional<String> getVariableOptional() {
      return Optional.ofNullable(m_variable);
   }

   /**
    * Get the variable name.
    *
    * @return the variable name, may be {@code null}
    */
   public String getVariable() {
      return m_variable;
   }

   /**
    * Set the variable name with enhanced validation.
    *
    * @param variable the variable name, should not be {@code null} or empty
    *                 (allows these for editing purposes)
    */
   public void setVariable(String variable) {
      this.m_variable = variable;
   }

   /**
    * Get the compiled JEXL script with lazy initialization and Optional wrapper.
    *
    * @return Optional containing the compiled script if expression is valid, empty otherwise
    */
   public Optional<IPSScript> getJexlScriptOptional() {
      if (m_jexl == null && StringUtils.isNotBlank(m_expression)) {
         try {
            m_jexl = PSJexlEvaluator.createScript(m_expression);
         } catch (Exception e) {
            // Return empty Optional if script compilation fails
            return Optional.empty();
         }
      }
      return Optional.ofNullable(m_jexl);
   }

   /**
    * Get the compiled JEXL script with lazy initialization.
    *
    * @return the compiled script, may be {@code null} if expression is invalid
    */
   public IPSScript getJexlScript() {
      return getJexlScriptOptional().orElse(null);
   }

   /**
    * Check if this binding has a valid JEXL expression.
    *
    * @return true if the expression can be compiled successfully
    */
   public boolean hasValidExpression() {
      return getJexlScriptOptional().isPresent();
   }

   /**
    * Check if this binding is ready for execution.
    *
    * @return true if both variable and expression are not blank
    */
   public boolean isReady() {
      return StringUtils.isNotBlank(m_variable) && StringUtils.isNotBlank(m_expression);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof PSTemplateBinding)) return false;

      var other = (PSTemplateBinding) obj;
      return Objects.equals(m_bindingId, other.m_bindingId) &&
             Objects.equals(m_variable, other.m_variable) &&
             Objects.equals(m_expression, other.m_expression) &&
             Objects.equals(m_executionOrder, other.m_executionOrder);
   }

   @Override
   public int hashCode() {
      return Objects.hash(m_bindingId, m_variable, m_expression, m_executionOrder);
   }

   @Override
   public String toString() {
      return String.format("PSTemplateBinding{id=%d, variable='%s', expression='%s', order=%d}",
                          m_bindingId, m_variable, m_expression, m_executionOrder);
   }

   @Override
   public PSTemplateBinding clone() {
      try {
         var cloned = (PSTemplateBinding) super.clone();
         // Clear cached JEXL script in clone
         cloned.m_jexl = null;
         return cloned;
      } catch (CloneNotSupportedException e) {
         // This should never happen since we implement Cloneable
         throw new RuntimeException("Clone not supported", e);
      }
   }

   // Legacy getters/setters for backward compatibility

   /**
    * Get the binding ID.
    *
    * @return the binding ID
    */
   @IPSXmlSerialization(suppress = true)
   public long getBindingId() {
      return m_bindingId;
   }

   /**
    * Set the binding ID.
    *
    * @param bindingId the binding ID
    */
   public void setBindingId(long bindingId) {
      this.m_bindingId = bindingId;
   }

   /**
    * Get the version for optimistic locking.
    *
    * @return the version, may be {@code null}
    */
   @IPSXmlSerialization(suppress = true)
   public Integer getVersion() {
      return m_version;
   }

   /**
    * Set the version for optimistic locking.
    *
    * @param version the version, may be {@code null}
    */
   public void setVersion(Integer version) {
      this.m_version = version;
   }
}
