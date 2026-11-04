export const WelcomePage = {
    template: `
        <section>
            <h2>Home</h2>
            <div class="pc-hero">
                <div>
                    <h1>Welcome to PetClinic</h1>
                    <p>Look after owners, their pets and every visit in one place. Records are served by three independent microservices, resolved through the API gateway.</p>
                    <div class="pc-hero__actions">
                        <router-link class="btn btn-primary" to="/owners">Find owners</router-link>
                        <router-link class="btn btn-ghost" to="/vets">Meet the veterinarians</router-link>
                    </div>
                </div>
                <img class="pc-hero__art" src="/images/pets.png" alt="pets logo">
            </div>
        </section>
    `
};
