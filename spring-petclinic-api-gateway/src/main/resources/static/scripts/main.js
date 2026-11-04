import { createApp } from 'vue';

import { RootApp } from './RootApp.js';
import { AppFooter } from './components/AppFooter.js';
import { AppNavigation } from './components/AppNavigation.js';
// ChatBox retiré : il appelait /api/genai/chatclient, route supprimée avec le genai-service.
// Le composant reste sur le disque (components/ChatBox.js) pour une éventuelle réactivation.
import { LoadFailure, LoadingSpinner } from './components/LoadingState.js';
import { router } from './router.js';

const application = createApp(RootApp);
application.component('loading-spinner', LoadingSpinner);
application.component('load-failure', LoadFailure);
application.component('navigation-bar', AppNavigation);
application.component('app-footer', AppFooter);
application.use(router);
application.mount('#app');
