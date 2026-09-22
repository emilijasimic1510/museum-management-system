package rs.ac.uns.ftn.museum.catalog;

import org.springframework.test.context.ActiveProfiles;

/** Run the exact same public/write authorization contract with the local profile enabled. */
@ActiveProfiles(value={"test","local"}, inheritProfiles=false)
class LocalExhibitCatalogIntegrationTests extends ExhibitCatalogIntegrationTests {}
