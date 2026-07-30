/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.rest.test.apibridge;

import com.percussion.rest.cecontrols.ControlDef;
import com.percussion.rest.cecontrols.IControlAdaptor;
import java.util.List;
import org.springframework.stereotype.Component;

/** Test adaptor for CE Controls API bridge (MainTest Spring context). */
@Component
public class TestControlAdaptor implements IControlAdaptor {

  @Override
  public List<ControlDef> listControls() {
    return List.of();
  }

  @Override
  public ControlDef findControlByName(String name) {
    return null;
  }
}
