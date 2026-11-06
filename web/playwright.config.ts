import {defineConfig} from '@playwright/test';
export default defineConfig({testDir:'./tests/e2e',workers:1,reporter:[['list'],['html',{open:'never'}]],use:{launchOptions:{executablePath:process.env.PLAYWRIGHT_CHROME_EXECUTABLE},channel:'chromium',screenshot:'only-on-failure',trace:'retain-on-failure',baseURL:'http://127.0.0.1:5173',viewport:{width:390,height:844}},
 webServer:{command:'npm run dev',url:'http://127.0.0.1:5173',env:{VITE_FIREBASE_EMULATORS:'true'},reuseExistingServer:!process.env.CI}});
