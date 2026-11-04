import { ref } from 'vue';
import { useRoute } from 'vue-router';

import { request, requireObject } from '../api/client.js';
import { formatDate } from '../utils/dates.js';
import { initials } from '../utils/initials.js';
import { useRequest } from '../utils/useRequest.js';

export const OwnerDetailsPage = {
    setup() {
        const route = useRoute();
        const owner = ref(null);
        const state = useRequest(async ({ signal }) => {
            owner.value = requireObject(await request(`/api/gateway/owners/${route.params.ownerId}`, { signal }), 'Owner not found.');
        });

        return {
            owner,
            formatDate,
            initials,
            loading: state.loading,
            failed: state.failed,
            failure: state.failure
        };
    },
    template: `
        <section>
            <loading-spinner v-if="loading"></loading-spinner>
            <load-failure v-else-if="failed" :error="failure" resource="Owner details"></load-failure>
            <div v-else-if="owner">
                <div class="pc-profile">
                    <span class="pc-avatar pc-avatar--lg">{{ initials(owner.firstName, owner.lastName) }}</span>
                    <div class="pc-row-body">
                        <h1 class="pc-profile__name">{{ owner.firstName }} {{ owner.lastName }}</h1>
                        <p class="pc-profile__meta">{{ owner.address }} · {{ owner.city }} · {{ owner.telephone }}</p>
                        <div class="pc-profile__actions">
                            <router-link class="btn btn-primary btn-sm" :to="'/owners/' + owner.id + '/edit'">Edit owner</router-link>
                            <router-link class="btn btn-ghost btn-sm" :to="'/owners/' + owner.id + '/new-pet'">Add a pet</router-link>
                        </div>
                    </div>
                </div>
                <h2>Pets and visits</h2>
                <div class="pc-list">
                    <article v-for="pet in owner.pets" :key="pet.id" class="pc-pet">
                        <header class="pc-pet__head">
                            <span class="pc-avatar">{{ initials(pet.name, '') }}</span>
                            <div class="pc-row-body">
                                <div class="pc-row-title">{{ pet.name }}</div>
                                <div class="pc-row-meta">{{ pet.type.name }} · born {{ formatDate(pet.birthDate) }}</div>
                            </div>
                            <div class="pc-pet__actions">
                                <router-link class="pc-link" :to="'/owners/' + owner.id + '/pets/' + pet.id">Edit pet</router-link>
                                <router-link class="pc-link" :to="'/owners/' + owner.id + '/pets/' + pet.id + '/visits'">Add visit</router-link>
                            </div>
                        </header>
                        <ul v-if="pet.visits.length" class="pc-visits">
                            <li v-for="visit in pet.visits" :key="visit.id">
                                <span class="pc-date">{{ formatDate(visit.date) }}</span>
                                <span>{{ visit.description }}</span>
                            </li>
                        </ul>
                        <p v-else class="pc-empty">No visit recorded yet.</p>
                    </article>
                </div>
            </div>
        </section>
    `
};
