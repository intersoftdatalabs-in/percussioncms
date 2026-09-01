/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

package com.percussion.rest.test.apibridge;

import com.percussion.rest.cecontrols.ControlDef;
import com.percussion.rest.cecontrols.IControlAdaptor;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Test adaptor for CE Controls API bridge (MainTest Spring context). */
@Component
@Lazy
public class TestControlAdaptor implements IControlAdaptor {

  @Override
  public List<ControlDef> listControls() {
    return List.of();
  }

  @Override
  public ControlDef findControlByName(String name) {
    return null;
  }

  @Override
  public ControlDef createControl(ControlDef body) {
    return body;
  }

  @Override
  public ControlDef saveControl(String name, ControlDef body) {
    return body;
  }

  @Override
  public boolean deleteControl(String name) {
    return false;
  }
}
