import Foundation
import Security

enum DeviceKeychain {
    private static let service = "info.chouteau.familyfit.device"
    static func load() throws -> DeviceAuthorization? {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service, kSecAttrAccount as String: "authorization", kSecReturnData as String: true, kSecMatchLimit as String: kSecMatchLimitOne]
        var item: CFTypeRef?
        let result = SecItemCopyMatching(query as CFDictionary, &item)
        if result == errSecItemNotFound { return nil }
        guard result == errSecSuccess, let data = item as? Data else { throw FamilyError.message("Déverrouille l’iPhone pour accéder à ton autorisation FamilyFit.") }
        return try JSONDecoder().decode(DeviceAuthorization.self, from: data)
    }
    static func save(_ value: DeviceAuthorization) throws {
        let data = try JSONEncoder().encode(value)
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service, kSecAttrAccount as String: "authorization"]
        let update = SecItemUpdate(query as CFDictionary, [kSecValueData as String: data] as CFDictionary)
        if update == errSecSuccess { return }
        guard update == errSecItemNotFound else { throw FamilyError.message("Impossible d’enregistrer l’autorisation dans le trousseau de l’iPhone.") }
        var attributes = query
        attributes[kSecValueData as String] = data
        attributes[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        guard SecItemAdd(attributes as CFDictionary, nil) == errSecSuccess else { throw FamilyError.message("Impossible de sécuriser ce téléphone.") }
    }
}
enum FamilyError: LocalizedError {
    case message(String)
    case http(Int, String)
    var errorDescription: String? { switch self { case .message(let text), .http(_, let text): return text } }
}
