export const AppNavigation = {
    template: `
        <nav class="side-rail" role="navigation" aria-label="Main">
            <router-link class="side-rail__brand" to="/" title="PetClinic home">
                <span class="side-rail__mark"><i class="fa fa-paw" aria-hidden="true"></i></span>
                <span class="side-rail__name">PetClinic</span>
            </router-link>
            <router-link class="side-rail__link" exact-active-class="is-active" to="/" title="Home">
                <i class="fa fa-home" aria-hidden="true"></i><span class="side-rail__label">Home</span>
            </router-link>
            <router-link class="side-rail__link" active-class="is-active" to="/owners" title="Find owners">
                <i class="fa fa-search" aria-hidden="true"></i><span class="side-rail__label">Find owners</span>
            </router-link>
            <router-link class="side-rail__link" active-class="is-active" to="/vets" title="Veterinarians">
                <i class="fa fa-stethoscope" aria-hidden="true"></i><span class="side-rail__label">Veterinarians</span>
            </router-link>
            <router-link class="side-rail__cta" to="/owners/new" title="Register owner">Register owner</router-link>
            <p class="side-rail__foot"><strong>PetClinic</strong><br>Owners, visits and veterinarians behind a Spring Cloud gateway.</p>
        </nav>`
};
