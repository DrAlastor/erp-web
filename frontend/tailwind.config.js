/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: 'var(--primary)',
          hover: 'var(--primary-hover)',
          active: 'var(--primary-active)',
          light: 'var(--primary-light)',
        },
        apple: {
          blue: '#007AFF',
          dark: '#1D1D1F',
          gray: '#F5F5F7',
          silver: '#86868B',
          muted: '#AAAAAA',
          red: '#FF3B30',
          green: '#34C759',
        },
        surface: 'var(--bg-surface)',
        subtle: 'var(--bg-subtle)',
      },
      fontFamily: {
        sans: ['Inter', '-apple-system', 'BlinkMacSystemFont', 'SF Pro Display', 'SF Pro Text', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      minHeight: {
        dvh: '100dvh',
      },
    },
  },
  plugins: [],
};
