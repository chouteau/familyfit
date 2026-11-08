using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models.Equipment;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Handlers.Equipment;

public sealed class SaveEquipmentImageRequestHandler(IDbContextFactory<FamilyDb> factory, AuthService auth, IMediator mediator)
    : AuthorizedHandler<SaveEquipmentImageRequest, EquipmentDefinition>(auth, mediator)
{
    public const int MaxImageBytes = 2 * 1024 * 1024;

    protected override async Task<Outcome<EquipmentDefinition>> Execute(SaveEquipmentImageRequest request, Actor actor, CancellationToken ct)
    {
        if (actor.AgentId == null) return Outcome<EquipmentDefinition>.Fail("Les photos se modifient uniquement via le serveur MCP.", 403);
        var input = request.Input;
        if (input == null || string.IsNullOrWhiteSpace(request.EquipmentId) || input.Revision < 1)
            return Outcome<EquipmentDefinition>.Fail("Fournis l’identifiant et la révision actuelle de l’équipement.");
        byte[]? bytes = null;
        if (input.Remove)
        {
            if (input.Base64 != null || input.ContentType != null) return Outcome<EquipmentDefinition>.Fail("Pour supprimer une photo, utilise remove=true sans base64 ni contentType.");
        }
        else
        {
            if (string.IsNullOrEmpty(input.Base64) || input.Base64.Length > ((MaxImageBytes + 2) / 3) * 4)
                return Outcome<EquipmentDefinition>.Fail("Envoie une image PNG, JPEG ou WebP en base64 brut, de 2 Mio maximum.");
            try { bytes = Convert.FromBase64String(input.Base64); }
            catch (FormatException) { return Outcome<EquipmentDefinition>.Fail("Le contenu de l’image n’est pas un base64 valide."); }
            if (bytes.Length > MaxImageBytes || !MatchesFormat(bytes, input.ContentType))
                return Outcome<EquipmentDefinition>.Fail("Image non reconnue : PNG, JPEG ou WebP uniquement, 2 Mio maximum. Le type MIME doit correspondre au contenu.");
        }
        await using var db = await factory.CreateDbContextAsync(ct);
        var equipment = await db.Equipment.SingleOrDefaultAsync(x => x.Id == request.EquipmentId, ct);
        if (equipment == null) return Outcome<EquipmentDefinition>.Fail("Équipement introuvable.", 404);
        if (equipment.Revision != input.Revision) return Outcome<EquipmentDefinition>.Fail("La fiche a changé. Relis le catalogue avant de réessayer.", 409);
        var image = await db.EquipmentImages.SingleOrDefaultAsync(x => x.EquipmentId == equipment.Id, ct);
        if (input.Remove)
        {
            if (image != null) db.EquipmentImages.Remove(image);
            equipment.ImageVersion = null;
        }
        else
        {
            if (image == null) { image = new EquipmentImage { EquipmentId = equipment.Id }; db.EquipmentImages.Add(image); }
            image.Data = bytes!;
            image.ContentType = input.ContentType!;
            equipment.ImageVersion = Guid.NewGuid();
        }
        equipment.Revision++;
        await db.SaveChangesAsync(ct);
        return Outcome<EquipmentDefinition>.Ok(equipment);
    }

    // Raster bytes are served as-is, never evaluated or decoded on the server.
    private static bool MatchesFormat(byte[] bytes, string? type)
    {
        var data = bytes.AsSpan();
        return type switch
        {
            "image/png" => data.Length >= 45 && data[..8].SequenceEqual(new byte[] { 137, 80, 78, 71, 13, 10, 26, 10 })
                && data.Slice(12, 4).SequenceEqual("IHDR"u8) && data.Slice(data.Length - 8, 4).SequenceEqual("IEND"u8),
            "image/jpeg" => data.Length >= 4 && data[0] == 0xff && data[1] == 0xd8 && data[^2] == 0xff && data[^1] == 0xd9,
            "image/webp" => data.Length >= 20 && data[..4].SequenceEqual("RIFF"u8) && data.Slice(8, 4).SequenceEqual("WEBP"u8)
                && (data.Slice(12, 4).SequenceEqual("VP8 "u8) || data.Slice(12, 4).SequenceEqual("VP8L"u8) || data.Slice(12, 4).SequenceEqual("VP8X"u8))
                && System.Buffers.Binary.BinaryPrimitives.ReadUInt32LittleEndian(data.Slice(4, 4)) == data.Length - 8,
            _ => false
        };
    }
}
