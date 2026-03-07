import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import App from '../../main/ts/components/App';

describe('App Component', () => {
  it('renders without crashing', () => {
    const { container } = render(<App />);
    expect(container).toBeTruthy();
  });

  it('displays the application title', () => {
    render(<App />);
    const title = screen.getByText(/Percussion CMS/i);
    expect(title).toBeInTheDocument();
  });

  it('displays Phase 4 message', () => {
    render(<App />);
    const phase4Message = screen.getByText(/Phase 4/i);
    expect(phase4Message).toBeInTheDocument();
  });

  it('displays components list', () => {
    render(<App />);
    const componentsList = screen.getByText(/Components Available/i);
    expect(componentsList).toBeInTheDocument();
  });
});
