package com.allvote.services;

import com.allvote.dtos.organisateur.InscriptionOrganisateurRequest;
import com.allvote.dtos.organisateur.InscriptionResponse;

public interface InscriptionService {

    InscriptionResponse inscrire(InscriptionOrganisateurRequest request);
}
