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
        },
        'dark-navy': {
          DEFAULT: 'var(--dark-navy)',
          hover: 'var(--dark-navy-hover)',
          surface: 'var(--dark-navy-surface)',
        },
      },
      fontFamily: {
        sans: ['Plus Jakarta Sans', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      minHeight: {
        dvh: '100dvh',
      },
    },
  },
  plugins: [],
};
