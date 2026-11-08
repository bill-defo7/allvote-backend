package com.allvote.services.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allvote.dtos.commun.PageDto;
import com.allvote.dtos.orga.FinanceConcoursDto;
import com.allvote.dtos.orga.FinancesOrgaDto;
import com.allvote.dtos.orga.TransactionOrgaDto;
import com.allvote.entities.Organisateur;
import com.allvote.entities.TransactionVote;
import com.allvote.repositories.ReversementRepository;
import com.allvote.repositories.TransactionVoteRepository;
import com.allvote.services.OrgaFinanceService;
import com.allvote.services.ParametreService;
import com.allvote.utiles.StatutReversement;
import com.allvote.utiles.StatutVote;
import com.allvote.utiles.Telephones;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrgaFinanceServiceImpl implements OrgaFinanceService {

    private final AccesOrganisateur acces;
    private final TransactionVoteRepository transactionRepository;
    private final ReversementRepository reversementRepository;
    private final ParametreService parametreService;

    @Override
    public PageDto<TransactionOrgaDto> transactions(Long idOrganisateur, StatutVote statut, Long idConcours,
            int page, int taille) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(taille, 1), 100),
                Sort.by(Sort.Direction.DESC, "dateCreation"));
        Page<TransactionVote> p;
        if (statut != null && idConcours != null) {
            p = transactionRepository.findByOrganisateur_IdOrganisateurAndStatutAndConcours_IdConcours(idOrganisateur, statut, idConcours, pr);
        } else if (statut != null) {
            p = transactionRepository.findByOrganisateur_IdOrganisateurAndStatut(idOrganisateur, statut, pr);
        } else if (idConcours != null) {
            p = transactionRepository.findByOrganisateur_IdOrganisateurAndConcours_IdConcours(idOrganisateur, idConcours, pr);
        } else {
            p = transactionRepository.findByOrganisateur_IdOrganisateur(idOrganisateur, pr);
        }
        return PageDto.de(p, p.getContent().stream().map(OrgaFinanceServiceImpl::vers).toList());
    }

    @Override
    public FinancesOrgaDto finances(Long idOrganisateur) {
        Organisateur o = acces.organisateur(idOrganisateur);
        List<FinanceConcoursDto> parConcours = transactionRepository
                .totauxParConcours(idOrganisateur, StatutVote.CONFIRME).stream()
                .map(l -> new FinanceConcoursDto(n(l[0]), (String) l[1], n(l[2]), n(l[3]), n(l[4]), n(l[5]), n(l[6])))
                .toList();

        long nb = parConcours.stream().mapToLong(FinanceConcoursDto::nbTransactions).sum();
        long voix = parConcours.stream().mapToLong(FinanceConcoursDto::voix).sum();
        long encaisse = parConcours.stream().mapToLong(FinanceConcoursDto::encaisse).sum();
        long commission = parConcours.stream().mapToLong(FinanceConcoursDto::commission).sum();
        long part = parConcours.stream().mapToLong(FinanceConcoursDto::partOrganisateur).sum();

        return new FinancesOrgaDto(
                o.getTauxCommission() != null ? o.getTauxCommission() : parametreService.tauxCommissionDefaut(),
                nb, voix, encaisse, commission, part,
                reversementRepository.somme(idOrganisateur, StatutReversement.PAYE),
                reversementRepository.somme(idOrganisateur, StatutReversement.A_PAYER),
                transactionRepository.nonReverse(idOrganisateur, StatutVote.CONFIRME),
                transactionRepository.countByOrganisateur_IdOrganisateurAndStatut(idOrganisateur, StatutVote.A_VERIFIER),
                parConcours);
    }

    private static TransactionOrgaDto vers(TransactionVote t) {
        return new TransactionOrgaDto(t.getReference(), t.getDateCreation(), t.getDateConfirmation(),
                t.getConcours().getIdConcours(), t.getConcours().getTitre(),
                t.getCandidat().getIdCandidat(), t.getCandidat().getNom(),
                t.getNbVoix(), t.getMontant() - t.getFraisService(), t.getTauxCommission(), t.getMontantCommission(), t.getMontantOrganisateur(),
                t.getOperateur(), Telephones.masquer(t.getTelephone()), t.getStatut(), t.getReversement() != null);
    }

    private static long n(Object o) {
        return o == null ? 0 : ((Number) o).longValue();
    }
}
