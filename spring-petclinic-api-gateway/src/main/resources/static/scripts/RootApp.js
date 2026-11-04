import { clearError, errorState } from './state/errors.js';

export const RootApp = {
    setup() {
        return {
            errorState,
            clearError
        };
    },
    template: `
        <div class="app-shell">
            <navigation-bar></navigation-bar>
            <main class="app-main">
                <div class="app-column">
                    <div v-if="errorState.message" class="alert alert-danger alert-dismissible" role="alert">
                        <button type="button" class="btn-close" aria-label="Close" @click="clearError"></button>
                        <div>{{ errorState.message }}</div>
                        <ul v-if="errorState.details.length"><li v-for="detail in errorState.details" :key="detail">{{ detail }}</li></ul>
                    </div>
                    <router-view></router-view>
                </div>
                <aside class="app-aside">
                    <section class="pc-card">
                        <h2 class="pc-card__title">About PetClinic</h2>
                        <p>A three-service reference application: owners, visits and veterinarians, each reached through the API gateway and registered in Eureka.</p>
                    </section>
                    <section class="pc-card">
                        <h2 class="pc-card__title">Services</h2>
                        <div class="pc-svc"><span class="pc-dot"></span>Customers<span class="pc-svc__meta">:8081</span></div>
                        <div class="pc-svc"><span class="pc-dot"></span>Visits<span class="pc-svc__meta">:8082</span></div>
                        <div class="pc-svc"><span class="pc-dot"></span>Vets<span class="pc-svc__meta">:8083</span></div>
                    </section>
                    <section class="pc-card">
                        <h2 class="pc-card__title">Stack</h2>
                        <div class="pc-row-sub">
                            <span class="pc-chip">Spring Boot</span>
                            <span class="pc-chip">Spring Cloud</span>
                            <span class="pc-chip">Vue 3</span>
                        </div>
                    </section>
                </aside>
            </main>
            <app-footer></app-footer>
        </div>`
};
