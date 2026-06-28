package com.teleconnect.iam;

import com.teleconnect.iam.entity.Permission;
import com.teleconnect.iam.entity.Role;
import com.teleconnect.iam.entity.User;
import com.teleconnect.iam.repository.UserRepository;
import com.teleconnect.iam.repository.PermissionRepository;
import com.teleconnect.iam.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component // must run before SubscriberSeeder (it needs the "S" role to exist)
public class DataLoader implements CommandLineRunner {

    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;  
    public DataLoader(RoleRepository roleRepo, PermissionRepository permRepo, UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.roleRepo = roleRepo;
        this.permRepo = permRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    
    @Value("${app.default.admin.email}")
    private String adminEmail;

    @Value("${app.default.admin.password}")
    private String adminPassword;


    @Override
    public void run(String... args) {

        // 1. Create all 18 permissions if they do not exist
        List<String> permNames = List.of(
            "VIEW_OWN_PLAN", "PAY_BILL", "RAISE_SERVICE_REQUEST",
            "VIEW_SUBSCRIBER", "CREATE_FAULT_TICKET", "UPDATE_FAULT_TICKET",
            "VIEW_INVOICE", "EDIT_INVOICE", "RAISE_DISPUTE",
            "VIEW_NETWORK_FAULTS", "CLOSE_FAULT_TICKET",
            "VIEW_AUDIT_LOGS", "VIEW_REPORTS", "VIEW_KYC",
            "CREATE_USER", "DELETE_USER", "MANAGE_PLANS", "VIEW_ALL_USERS"
        );

        Map<String, Permission> permMap = new HashMap<>();
        for (String name : permNames) {
            Permission p = permRepo.findByPermissionName(name).orElseGet(() -> {
                Permission np = new Permission();
                np.setPermissionName(name);
                return permRepo.save(np);
            });
            permMap.put(name, p);
        }

        // Helper
        java.util.function.Function<String[], List<Permission>> perms =
            names -> Arrays.stream(names).map(permMap::get).toList();

        // 2. Create all 6 roles with their permissions if they do not exist
        createRole("S",   perms.apply(new String[]{"VIEW_OWN_PLAN", "PAY_BILL", "RAISE_SERVICE_REQUEST"}));
        createRole("CS",  perms.apply(new String[]{"VIEW_SUBSCRIBER", "CREATE_FAULT_TICKET", "UPDATE_FAULT_TICKET"}));
        createRole("B",   perms.apply(new String[]{"VIEW_INVOICE", "EDIT_INVOICE", "RAISE_DISPUTE"}));
        createRole("N",   perms.apply(new String[]{"VIEW_NETWORK_FAULTS", "CLOSE_FAULT_TICKET"}));
        createRole("C",   perms.apply(new String[]{"VIEW_AUDIT_LOGS", "VIEW_REPORTS", "VIEW_KYC"}));
        createRole("A",   perms.apply(new String[]{"CREATE_USER", "DELETE_USER", "MANAGE_PLANS", "VIEW_ALL_USERS","VIEW_AUDIT_LOGS"}));

        System.out.println("[TeleConnect IAM] Roles and permissions seeded successfully.");
    

    
        if  (!userRepo.existsByEmail(adminEmail)) {

            Role adminRole = roleRepo.findByRoleName("A")
                .orElseThrow(() -> new RuntimeException("Role A not found"));

            User admin = new User();
            admin.setName("System Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setPhone("9000000000");
            admin.setRole(adminRole);
            admin.setRegionId(1);
            admin.setStatus(User.Status.A);
            admin.setMustChangePassword(true);
        
            userRepo.save(admin);

            System.out.println("[TeleConnect] Default admin created: " + adminEmail);
        }

    }

    private void createRole(String name, List<Permission> permissions) {
        if (roleRepo.findByRoleName(name).isEmpty()) {
            Role role = new Role();
            role.setRoleName(name);
            role.setPermissions(permissions);
            roleRepo.save(role);
        }
    }
}
