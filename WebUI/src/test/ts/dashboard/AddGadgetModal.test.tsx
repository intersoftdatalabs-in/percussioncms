/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import React from 'react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AddGadgetModal } from '@/dashboard/AddGadgetModal';

describe('AddGadgetModal', () => {
  const mockAvailableGadgets = [
    { id: '1', name: 'Gadget A', description: 'Description A', category: 'Analytics' },
    { id: '2', name: 'Gadget B', description: 'Description B', category: 'Reports' },
    { id: '3', name: 'Gadget C', description: 'Description C', category: 'Analytics' },
    { id: '4', name: 'Gadget D', description: 'Description D', category: 'Tools' },
  ];

  const mockOnAdd = vi.fn();
  const mockOnClose = vi.fn();

  beforeEach(() => {
    mockOnAdd.mockClear();
    mockOnClose.mockClear();
  });

  it('should not render when isOpen is false', () => {
    const { container } = render(
      <AddGadgetModal
        isOpen={false}
        availableGadgets={mockAvailableGadgets}
        activeGadgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    expect(container.firstChild).toBeNull();
  });

  it('should render when isOpen is true', () => {
    render(
      <AddGadgetModal
        isOpen={true}
        availableGadgets={mockAvailableGadgets}
        activeGadgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    expect(screen.getByText('Add Gadget')).toBeDefined();
  });

  it('should display all available gadgets', () => {
    render(
      <AddGadgetModal
        isOpen={true}
        availableGadgets={mockAvailableGadgets}
        activeGadgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    expect(screen.getByText('Gadget A')).toBeDefined();
    expect(screen.getByText('Gadget B')).toBeDefined();
    expect(screen.getByText('Gadget C')).toBeDefined();
    expect(screen.getByText('Gadget D')).toBeDefined();
  });

  it('should group gadgets by category', () => {
    render(
      <AddGadgetModal
        isOpen={true}
        availableGadgets={mockAvailableGadgets}
        activeGadgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    expect(screen.getByText('Analytics')).toBeDefined();
    expect(screen.getByText('Reports')).toBeDefined();
    expect(screen.getByText('Tools')).toBeDefined();
  });

  it('should filter gadgets by search term', async () => {
    render(
      <AddGadgetModal
        isOpen={true}
        availableGadgets={mockAvailableGadgets}
        activeGadgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    const searchInput = screen.getByPlaceholderText('Search gadgets...');
    fireEvent.change(searchInput, { target: { value: 'Gadget A' } });

    await waitFor(() => {
      expect(screen.getByText('Gadget A')).toBeDefined();
      expect(screen.queryByText('Gadget B')).toBeNull();
    });
  });

  it('should filter gadgets by description', async () => {
    render(
      <AddGadgetModal
        isOpen={true}
        availableGadgets={mockAvailableGadgets}
        activeGadgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    const searchInput = screen.getByPlaceholderText('Search gadgets...');
    fireEvent.change(searchInput, { target: { value: 'Description B' } });

    await waitFor(() => {
      expect(screen.getByText('Gadget B')).toBeDefined();
    });
  });

  it('should call onAdd when add button is clicked', async () => {
    render(
      <AddGadgetModal
        isOpen={true}
        availableGadgets={mockAvailableGadgets}
        activeGadgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    const addButtons = screen.getAllByRole('button').filter((btn) => btn.textContent === 'Add');
    fireEvent.click(addButtons[0]);

    expect(mockOnAdd).toHaveBeenCalledWith('1');
  });

  it('should disable add button for active gadgets', () => {
    render(
      <AddGadgetModal
        isOpen={true}
        availableGadgets={mockAvailableGadgets}
        activeGadgetIds={new Set(['1', '2'])}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    // Gadgets are grouped by category (not id order); assert by label + disabled.
    const addedButtons = screen
      .getAllByRole('button')
      .filter((btn) => btn.textContent === 'Added');
    expect(addedButtons).toHaveLength(2);
    for (const btn of addedButtons) {
      expect((btn as HTMLButtonElement).disabled).toBe(true);
    }
    const addButtons = screen
      .getAllByRole('button')
      .filter((btn) => btn.textContent === 'Add');
    expect(addButtons).toHaveLength(2);
  });

  it('should call onClose when close button is clicked', () => {
    render(
      <AddGadgetModal
        isOpen={true}
        availableGadgets={mockAvailableGadgets}
        activeGadgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    fireEvent.click(screen.getByText('✕'));
    expect(mockOnClose).toHaveBeenCalled();
  });

  it('should call onClose when clicking outside modal', () => {
    const { container } = render(
      <AddGadgetModal
        isOpen={true}
        availableGadgets={mockAvailableGadgets}
        activeGadgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    // Find the backdrop div
    const backdrop = container.querySelector('div[style*="position: fixed"]');
    if (backdrop) {
      fireEvent.click(backdrop);
      expect(mockOnClose).toHaveBeenCalled();
    }
  });

  it('should not call onClose when clicking inside modal content', () => {
    render(
      <AddGadgetModal
        isOpen={true}
        availableGadgets={mockAvailableGadgets}
        activeGadgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    const modalContent = screen.getByText('Add Gadget').parentElement;
    if (modalContent) {
      fireEvent.click(modalContent);
      expect(mockOnClose).not.toHaveBeenCalled();
    }
  });

  it('should display "No gadgets found" when search returns empty', async () => {
    render(
      <AddGadgetModal
        isOpen={true}
        availableGadgets={mockAvailableGadgets}
        activeGadgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    const searchInput = screen.getByPlaceholderText('Search gadgets...');
    fireEvent.change(searchInput, { target: { value: 'NonexistentGadget' } });

    await waitFor(() => {
      expect(screen.getByText('No gadgets found')).toBeDefined();
    });
  });

  it('should clear search when input is cleared', async () => {
    render(
      <AddGadgetModal
        isOpen={true}
        availableGadgets={mockAvailableGadgets}
        activeGadgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    const searchInput = screen.getByPlaceholderText('Search gadgets...') as HTMLInputElement;
    fireEvent.change(searchInput, { target: { value: 'Gadget A' } });

    await waitFor(() => {
      expect(searchInput.value).toBe('Gadget A');
    });

    fireEvent.change(searchInput, { target: { value: '' } });

    await waitFor(() => {
      expect(screen.getByText('Gadget B')).toBeDefined();
    });
  });

  it('should handle gadgets without category', () => {
    const gadgetsWithoutCategory = [
      { id: '1', name: 'Gadget No Cat', description: 'No category' },
      { id: '2', name: 'Gadget A', description: 'Description A', category: 'Analytics' },
    ];

    render(
      <AddGadgetModal
        isOpen={true}
        availableGadgets={gadgetsWithoutCategory}
        activeGadgetIds={new Set()}
        onAdd={mockOnAdd}
        onClose={mockOnClose}
      />
    );

    expect(screen.getByText('Gadget No Cat')).toBeDefined();
    expect(screen.getByText('Other')).toBeDefined(); // Default category
  });
});
