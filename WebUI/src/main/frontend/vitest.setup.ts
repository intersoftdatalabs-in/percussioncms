/**
 * Vitest Setup File
 *
 * This file configures test environment and global test utilities.
 * It runs before all tests to set up matchers and global test helpers.
 */

import '@testing-library/jest-dom';
import { expect, afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

// Cleanup after each test
afterEach(() => {
  cleanup();
});

// Extend expect matchers with testing-library custom matchers
// (Already registered via @testing-library/jest-dom import above)
