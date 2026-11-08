package com.allvote.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.allvote.dtos.vote.VoteRequest;
import com.allvote.dtos.vote.VoteResponse;
import com.allvote.dtos.vote.VoteStatutDto;
import com.allvote.services.VoteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Vote public (sans compte) : paiement Mobile Money. */
@RestController
@RequestMapping("/api/public/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VoteResponse voter(@Valid @RequestBody VoteRequest request) {
        return voteService.creer(request);
    }

    @GetMapping("/{reference}")
    public VoteStatutDto statut(@PathVariable UUID reference) {
        return voteService.statut(reference);
    }
}
