/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.services.pipeline.model;

import java.util.Objects;

/** Pipe stage skeleton for one resource (subset of classic query/update pipe). */
public class PipelineStagesIr {

  private PageTankStageIr pageTank = new PageTankStageIr();
  private BackendTankStageIr backendTank = new BackendTankStageIr();
  private MapperStageIr mapper = new MapperStageIr();
  private SelectorStageIr selector = new SelectorStageIr();
  private PagerStageIr pager = new PagerStageIr();
  private UpdaterStageIr updater = new UpdaterStageIr();

  public PageTankStageIr getPageTank() {
    return pageTank;
  }

  public void setPageTank(PageTankStageIr pageTank) {
    this.pageTank = pageTank != null ? pageTank : new PageTankStageIr();
  }

  public BackendTankStageIr getBackendTank() {
    return backendTank;
  }

  public void setBackendTank(BackendTankStageIr backendTank) {
    this.backendTank = backendTank != null ? backendTank : new BackendTankStageIr();
  }

  public MapperStageIr getMapper() {
    return mapper;
  }

  public void setMapper(MapperStageIr mapper) {
    this.mapper = mapper != null ? mapper : new MapperStageIr();
  }

  public SelectorStageIr getSelector() {
    return selector;
  }

  public void setSelector(SelectorStageIr selector) {
    this.selector = selector != null ? selector : new SelectorStageIr();
  }

  public PagerStageIr getPager() {
    return pager;
  }

  public void setPager(PagerStageIr pager) {
    this.pager = pager != null ? pager : new PagerStageIr();
  }

  public UpdaterStageIr getUpdater() {
    return updater;
  }

  public void setUpdater(UpdaterStageIr updater) {
    this.updater = updater != null ? updater : new UpdaterStageIr();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PipelineStagesIr that)) {
      return false;
    }
    return Objects.equals(pageTank, that.pageTank)
        && Objects.equals(backendTank, that.backendTank)
        && Objects.equals(mapper, that.mapper)
        && Objects.equals(selector, that.selector)
        && Objects.equals(pager, that.pager)
        && Objects.equals(updater, that.updater);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pageTank, backendTank, mapper, selector, pager, updater);
  }
}
