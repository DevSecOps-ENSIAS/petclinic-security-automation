import { computed, ref } from 'vue';

import { request, requireArray } from '../api/client.js';
import { initials } from '../utils/initials.js';
import { matchesQuery } from '../utils/matchesQuery.js';
import { useRequest } from '../utils/useRequest.js';

export const OwnersPage = {
    setup() {
        const owners = ref([]);
        const query = ref('');
        const state = useRequest(async ({ signal }) => {
            owners.value = requireArray(await request('/api/customer/owners', { signal }), 'Owners could not be loaded.');
        });
        const filteredOwners = computed(() => {
            const normalizedQuery = query.value.trim().toLowerCase();
            if (!normalizedQuery) {
                return owners.value;
            }
            return owners.value.filter((owner) => matchesQuery(owner, normalizedQuery));
        });

        return {
            owners,
            query,
            filteredOwners,
            initials,
            loading: state.loading,
            failed: state.failed,
            failure: state.failure
        };
    },
    template: `
        <section>
            <loading-spinner v-if="loading"></loading-spinner>
            <load-failure v-else-if="failed" :error="failure" resource="Owner data"></load-failure>
            <div v-else>
                <h2>Owners</h2>
                <div class="pc-search">
                    <i class="fa fa-search" aria-hidden="true"></i>
                    <label class="visually-hidden" for="owner-query">Search by last name</label>
                    <input id="owner-query" v-model="query" class="form-control" name="query" placeholder="Search by last name">
                </div>
                <p class="pc-hint pc-count">{{ filteredOwners.length }} of {{ owners.length }} owners</p>
                <div class="pc-list">
                    <router-link v-for="owner in filteredOwners" :key="owner.id" class="pc-row" :to="'/owners/details/' + owner.id">
                        <span class="pc-avatar">{{ initials(owner.firstName, owner.lastName) }}</span>
                        <div class="pc-row-body">
                            <div class="pc-row-title">{{ owner.firstName }} {{ owner.lastName }}</div>
                            <div class="pc-row-meta">{{ owner.address }} · {{ owner.city }} · {{ owner.telephone }}</div>
                            <div class="pc-row-sub">
                                <template v-if="owner.pets.length">
                                    <span v-for="pet in owner.pets" :key="pet.id" class="pc-chip">{{ pet.name }}</span>
                                </template>
                                <span v-else class="pc-chip pc-chip--muted">No pets</span>
                            </div>
                        </div>
                        <i class="fa fa-chevron-right pc-row-chevron" aria-hidden="true"></i>
                    </router-link>
                </div>
                <p v-if="!filteredOwners.length" class="pc-empty pc-empty--flush">No owner matches “{{ query }}”.</p>
            </div>
        </section>
    `
};
