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
 */
package org.apache.soap;

import java.util.Vector;
import org.w3c.dom.Element;

/** Minimal compatibility shim for legacy Axis-style Header used by PSWebServiceAgent. */
public class Header
{
    private Vector<Element> headerEntries;

    public Header()
    {
        this.headerEntries = new Vector<>();
    }

    public void setHeaderEntries(Vector<Element> entries)
    {
        this.headerEntries = entries;
    }

    public Vector<Element> getHeaderEntries()
    {
        return this.headerEntries;
    }
}
