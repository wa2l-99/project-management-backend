package com.pmt.project_management.user;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pmt.project_management.history.TaskModifiedHistory;
import com.pmt.project_management.project.Project;
import com.pmt.project_management.role.Role;
import com.pmt.project_management.task.Task;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "_user")
@EntityListeners(AuditingEntityListener.class)
public class User implements UserDetails, Principal {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nom;

    private String prenom;

    @Column(unique = true)
    private String email;

    private String password;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = true)
    private Role role;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime lastModifiedDate;

    @OneToMany(mappedBy = "owner")
    @JsonIgnore
    private Set<Project> projectsOwner = new HashSet<>();

    // Relation ManyToMany avec les projets (les utilisateurs peuvent être membres de plusieurs projets)
    @ManyToMany(mappedBy = "members")
    @JsonIgnore
    private Set<Project> projects = new HashSet<>();

    // Relation OneToMany pour les tâches assignées à l'utilisateur
    @OneToMany(mappedBy = "assignedTo")
    @JsonBackReference
    private Set<Task> assignedTasks = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private List<TaskModifiedHistory> histories;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role != null ?
                Set.of(new SimpleGrantedAuthority(role.getNom().name())) :
                Set.of();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return email;
    }


    public String getFullName() {
        return prenom + " " + nom;
    }


    // Redéfinir equals pour comparer les utilisateurs par leur ID ou email
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
