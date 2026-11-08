package com.allvote.dtos.admin;

import jakarta.validation.constraints.Size;

public record ReversementRequest(@Size(max = 250) String note) {
}
