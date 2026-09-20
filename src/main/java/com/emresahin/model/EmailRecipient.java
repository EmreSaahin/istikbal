package com.emresahin.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "email_recipients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String department;

    @Column
    private String status;
}
