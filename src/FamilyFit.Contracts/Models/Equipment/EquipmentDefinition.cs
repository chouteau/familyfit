using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Text.Json.Serialization;

namespace FamilyFit.Contracts.Models.Equipment;

public sealed class EquipmentDefinition
{
    [Key, StringLength(64)] public string Id { get; set; } = "";
    [Required, StringLength(80)] public string Name { get; set; } = "";
    [Required, StringLength(2000)] public string Description { get; set; } = "";
    [JsonIgnore] public string TargetMusclesJson { get; set; } = "[]";
    [NotMapped] public string[] TargetMuscles
    {
        get => JsonDefaults.Read<string[]>(TargetMusclesJson);
        set => TargetMusclesJson = JsonDefaults.Write(value);
    }
    [JsonIgnore] public Guid? ImageVersion { get; set; }
    [NotMapped] public string? ImageUrl => ImageVersion is { } version
        ? $"/media/equipment/{Uri.EscapeDataString(Id)}?v={version:N}" : null;
    public long Revision { get; set; } = 1;
}

// Keep binary data out of catalogue queries, MCP replies and normal JSON responses.
public sealed class EquipmentImage
{
    [Key, StringLength(64)] public string EquipmentId { get; set; } = "";
    [JsonIgnore] public byte[] Data { get; set; } = [];
    public string ContentType { get; set; } = "";
}

public sealed record EquipmentInput(string Id, string Name, string Description, string[] TargetMuscles, long Revision = 0);
public sealed record EquipmentImageInput(long Revision, string? Base64 = null, string? ContentType = null, bool Remove = false);
