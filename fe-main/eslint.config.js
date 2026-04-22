import js from '@eslint/js';
import react from 'eslint-plugin-react';
import prettier from 'eslint-plugin-prettier';

export default [
  js.configs.recommended,
  {
    plugins: {
      react,
      prettier,
    },
    rules: {
      'prettier/prettier': 'error',
    },
  },
];
