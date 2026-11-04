import { ref } from 'vue';

import { request, requireArray } from '../api/client.js';
import { initials } from '../utils/initials.js';
import { useRequest } from '../utils/useRequest.js';

export const VetsPage = {
    setup() {
        const vets = ref([]);
        const state = useRequest(async ({ signal }) => {
            vets.value = requireArray(await request('/api/vet/vets', { signal }), 'Veterinarians could not be loaded.');
        });

        return {
            vets,
            initials,
            loading: state.loading,
            failed: state.failed,
            failure: state.failure
        };
    },
    template: `
        <section>
            <loading-spinner v-if="loading"></loading-spinner>
            <load-failure v-else-if="failed" :error="failure" resource="Veterinarian data"></load-failure>
            <div v-else>
                <h2>Veterinarians</h2>
                <div class="pc-list">
                    <div v-for="vet in vets" :key="vet.id" class="pc-row">
                        <span class="pc-avatar">{{ initials(vet.firstName, vet.lastName) }}</span>
                        <div class="pc-row-body">
                            <div class="pc-row-title">{{ vet.firstName }} {{ vet.lastName }}</div>
                            <div class="pc-row-sub">
                                <template v-if="vet.specialties.length">
                                    <span v-for="specialty in vet.specialties" :key="specialty.name" class="pc-chip">{{ specialty.name }}</span>
                                </template>
                                <span v-else class="pc-chip pc-chip--muted">General practice</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    `
};
