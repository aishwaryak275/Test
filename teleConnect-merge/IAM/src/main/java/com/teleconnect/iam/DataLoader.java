package com.teleconnect.iam;

import com.teleconnect.iam.entity.Permission;
import com.teleconnect.iam.entity.Role;
import com.teleconnect.iam.repository.PermissionRepository;
import com.teleconnect.iam.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component // must run before SubscriberSeeder (it needs the "S" role to exist)
public class DataLoader implements CommandLineRunner {

    @Autowired private RoleRepository roleRepo;
    @Autowired private PermissionRepository permRepo;

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
        createRole("A",   perms.apply(new String[]{"CREATE_USER", "DELETE_USER", "MANAGE_PLANS", "VIEW_ALL_USERS"}));

        System.out.println("[TeleConnect IAM] Roles and permissions seeded successfully.");
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
