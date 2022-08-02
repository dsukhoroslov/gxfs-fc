package eu.gaiax.difs.fc.core.graph.storage;

public class SdClaim {
        String subject;
        String predicate;
        String object;



        public SdClaim()
        {
            this.subject="<https://delta-dao.com/.well-known/participantAmazon.json>";
            this.predicate="<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>";
            this.object="<https://json-ld.org/playground/LegalPerson>";

        }

        public SdClaim(String subject, String predicate, String object)
        {
            this.subject=subject;
            this.predicate=predicate;
            this.object=object;
        }
}

