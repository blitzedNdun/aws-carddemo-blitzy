// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import '@testing-library/jest-dom';

// Polyfills for MUI X DataGrid and other modern web APIs
if (typeof global.TextEncoder === 'undefined') {
  global.TextEncoder = class TextEncoder {
    encode(input) {
      return new Uint8Array(Buffer.from(input, 'utf8'));
    }

    encodeInto(input, destination) {
      const encoded = this.encode(input);
      const length = Math.min(encoded.length, destination.length);

      for (let i = 0; i < length; i++) {
        destination[i] = encoded[i];
      }

      return {
        read: input.length,
        written: length,
      };
    }
  };
}

if (typeof global.TextDecoder === 'undefined') {
  global.TextDecoder = class TextDecoder {
    decode(input) {
      return Buffer.from(input).toString('utf8');
    }
  };
}

// Mock ResizeObserver for MUI components
global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
};

// Mock IntersectionObserver for MUI components
global.IntersectionObserver = class IntersectionObserver {
  constructor() {}
  observe() {}
  unobserve() {}
  disconnect() {}
};
