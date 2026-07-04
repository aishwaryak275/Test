package com.teleconnect.iam;

import com.teleconnect.iam.entity.Permission;
import com.teleconnect.iam.entity.Role;
import com.teleconnect.iam.repository.PermissionRepository;
import com.teleconnect.iam.repository.RoleRepository;
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

    public DataLoader(RoleRepository roleRepo, PermissionRepository permRepo) {
        this.roleRepo = roleRepo;
        this.permRepo = permRepo;
    }

    @Override
    public void run(String... args) {

        // 1. Create all 18 permissions if they do not exist
        List<String> permNames = List.of(
            "VIEW_PLAN", "PAY_BILL", "RAISE_SERVICE_REQUEST",
            "VIEW_SUBSCRIBER", "CREATE_FAULT_TICKET", "UPDATE_FAULT_TICKET",
            "VIEW_INVOICE", "EDIT_INVOICE", "RAISE_DISPUTE",
            "VIEW_NETWORK_FAULTS", "CLOSE_FAULT_TICKET",
            "VIEW_AUDIT_LOGS", "VIEW_REPORTS", "VIEW_KYC",
            "CREATE_USER", "DELETE_USER", "MANAGE_PLANS", 
            "VIEW_ALL_USERS","USAGE_RECORDS","USAGE_ANALYTICS",
            "KYC_EXPIRE","CREATE_SUB","GET_SUB","BILLING_CYCLE",
            "BILLING_REPORT","BILLING_DISPUTE","EDIT_DISPUTE","CREATE_NOTIFICATION",
            "VIEW_NOTIFICATIONS","MARK_NOTIFICATION","SERVICE_REQUEST","GET_UPDATE_TICKET","RESOLVE_TICKET"
        );

        Map<String, Permission> permMap = new HashMap<>();
        for (String name : permNames) {
            try {
                Permission p = permRepo.findByPermissionName(name).orElseGet(() -> {
                    Permission np = new Permission();
                    np.setPermissionName(name);
                    return permRepo.save(np);
                });
                permMap.put(name, p);
                System.out.println("[TeleConnect IAM] Created permission: " + name);
            } catch (Exception e) {
                System.err.println("[TeleConnect IAM] ERROR creating permission '" + name + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("[TeleConnect IAM] Total permissions created: " + permMap.size() + " out of " + permNames.size());

        // Helper
        java.util.function.Function<String[], List<Permission>> perms =
            names -> Arrays.stream(names).map(permMap::get).toList();

        // 2. Create all 6 roles with their permissions if they do not exist
        createRole("S",   perms.apply(new String[]{"VIEW_PLAN", "PAY_BILL", "RAISE_SERVICE_REQUEST","USAGE_RECORDS","GET_SUB","VIEW_INVOICE","BILLING_DISPUTE","MARK_NOTIFICATION","VIEW_NOTIFICATIONS"}));
        createRole("CS",  perms.apply(new String[]{"VIEW_SUBSCRIBER", "CREATE_FAULT_TICKET", "UPDATE_FAULT_TICKET","USAGE_RECORDS","USAGE_ANALYTICS","VIEW_KYC","VIEW_PLAN","CREATE_SUB","GET_SUB","VIEW_NOTIFICATIONS","SERVICE_REQUEST","GET_UPDATE_TICKET"}));
        createRole("B",   perms.apply(new String[]{"VIEW_INVOICE", "EDIT_INVOICE", "RAISE_DISPUTE","USAGE_RECORDS","USAGE_ANALYTICS","VIEW_SUBSCRIBER","VIEW_PLAN","GET_SUB","BILLING_CYCLE","PAY_BILL","BILLING_REPORT","BILLING_DISPUTE","EDIT_DISPUTE","VIEW_NOTIFICATIONS"}));
        createRole("N",   perms.apply(new String[]{"VIEW_NETWORK_FAULTS", "CLOSE_FAULT_TICKET","USAGE_ANALYTICS","VIEW_PLAN","VIEW_NOTIFICATIONS","GET_UPDATE_TICKET","RESOLVE_TICKET"}));
        createRole("C",   perms.apply(new String[]{"VIEW_AUDIT_LOGS", "VIEW_REPORTS","USAGE_RECORDS","USAGE_ANALYTICS","VIEW_PLAN","GET_SUB","VIEW_NOTIFICATIONS"}));
        createRole("A",   perms.apply(new String[]{"CREATE_USER", "DELETE_USER", "MANAGE_PLANS", "VIEW_ALL_USERS", "USAGE_RECORDS","USAGE_ANALYTICS","VIEW_SUBSCRIBER","VIEW_KYC","KYC_EXPIRE","VIEW_PLAN",
                                                           "CREATE_SUB","GET_SUB","BILLING_CYCLE","BILLING_REPORT","VIEW_INVOICE", "EDIT_INVOICE",
                                                           "PAY_BILL","BILLING_DISPUTE","EDIT_DISPUTE","CREATE_NOTIFICATION","VIEW_NOTIFICATIONS","SERVICE_REQUEST","GET_UPDATE_TICKET","RESOLVE_TICKET"}));

        System.out.println("[TeleConnect IAM] Roles and permissions seeded successfully.");
    }

    private void createRole(String name, List<Permission> permissions) {
        try {
            var existingRole = roleRepo.findByRoleName(name);
            if (existingRole.isPresent()) {
                Role role = existingRole.get();
                // Update permissions for existing role
                role.setPermissions(permissions);
                roleRepo.save(role);
                System.out.println("[TeleConnect IAM] Updated role: " + name + " with " + permissions.size() + " permissions");
            } else {
                Role role = new Role();
                role.setRoleName(name);
                role.setPermissions(permissions);
                roleRepo.save(role);
                System.out.println("[TeleConnect IAM] Created role: " + name + " with " + permissions.size() + " permissions");
            }
        } catch (Exception e) {
            System.err.println("[TeleConnect IAM] ERROR creating/updating role '" + name + "': " + e.getMessage());
            e.printStackTrace();
        }
    }
}
