/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['index.html', 'fr/**/*.html', 'en/**/*.html', 'demo.html', 'src/**/*.{html,vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      screens: {
        // Matches breakpoints used across AppHeader, AppSidebar, DashboardFilters
        'md': '768px',
        'lg': '1200px',
      },
    },
  },
}
