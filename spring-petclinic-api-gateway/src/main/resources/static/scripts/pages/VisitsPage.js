import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { jsonRequest, request, requireArray } from '../api/client.js';
import { clearError, reportError } from '../state/errors.js';
import { today } from '../utils/dates.js';
import { useRequest } from '../utils/useRequest.js';
import { useSubmission } from '../utils/useSubmission.js';

export const VisitsPage = {
    setup() {
        const route = useRoute();
        const router = useRouter();
        const ownerId = route.params.ownerId;
        const petId = route.params.petId;
        const visits = ref([]);
        const form = reactive({
            date: today(),
            description: ''
        });
        const url = `/api/visit/owners/${ownerId}/pets/${petId}/visits`;
        const state = useRequest(async ({ signal }) => {
            visits.value = requireArray(await request(url, { signal }), 'Visits could not be loaded.');
        });
        const submission = useSubmission();

        async function submit(event) {
            if (!event.target.reportValidity() || submission.submitting.value) {
                return;
            }

            const errorContext = clearError();
            try {
                await submission.run(async ({ signal }) => {
                    await request(url, {
                        ...jsonRequest('POST', {
                            date: form.date,
                            description: form.description.trim()
                        }),
                        signal
                    });
                    await router.push(`/owners/details/${ownerId}`);
                });
            } catch (error) {
                if (!error.cancelled) {
                    reportError(error, errorContext);
                }
            }
        }

        return {
            form,
            visits,
            loading: state.loading,
            failed: state.failed,
            failure: state.failure,
            submitting: submission.submitting,
            clearError,
            submit
        };
    },
    template: `
        <section>
            <loading-spinner v-if="loading"></loading-spinner>
            <load-failure v-else-if="failed" :error="failure" resource="Visit data"></load-failure>
            <div v-else>
                <h2>Add a visit</h2>
                <div class="pc-card pc-card--form">
                    <form class="pc-form" @input="clearError" @submit.prevent="submit">
                        <div class="mb-3">
                            <label for="visit-date">Date</label>
                            <input id="visit-date" v-model="form.date" type="date" class="form-control" required>
                        </div>
                        <div class="mb-3">
                            <label for="visit-description">Description</label>
                            <textarea id="visit-description" v-model.trim="form.description" class="form-control" maxlength="8192" style="resize:vertical;" required></textarea>
                        </div>
                        <div class="pc-form-foot">
                            <button class="btn btn-primary" type="submit" :disabled="submitting">
                                {{ submitting ? 'Saving…' : 'Add visit' }}
                            </button>
                        </div>
                    </form>
                </div>
                <h2>Previous visits</h2>
                <div class="pc-list">
                    <div v-for="visit in visits" :key="visit.id" class="pc-row">
                        <span class="pc-date">{{ visit.date }}</span>
                        <div class="pc-row-body">
                            <div class="pc-row-meta" style="white-space: pre-line;">{{ visit.description }}</div>
                        </div>
                    </div>
                </div>
                <p v-if="!visits.length" class="pc-empty pc-empty--flush">No visit recorded yet.</p>
            </div>
        </section>
    `
};
