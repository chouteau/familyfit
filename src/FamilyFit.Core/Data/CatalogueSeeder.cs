using System.Reflection;
using FamilyFit.Contracts.Models;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Data;

public static class CatalogueSeeder
{
    public static async Task SeedAsync(FamilyDb db, CancellationToken ct = default)
    {
        await using var stream = typeof(CatalogueSeeder).Assembly.GetManifestResourceStream("FamilyFit.Core.Data.exercises.json")!;
        var exercises = await System.Text.Json.JsonSerializer.DeserializeAsync<List<Exercise>>(stream, JsonDefaults.Options, ct) ?? [];
        await using var transaction = await db.Database.BeginTransactionAsync(System.Data.IsolationLevel.Serializable, ct);
        var ids = await db.Exercises.Select(x => x.Id).ToListAsync(ct);
        db.Exercises.AddRange(exercises.Where(x => !ids.Contains(x.Id)));
        var equipmentIds = await db.Equipment.Select(x => x.Id).ToListAsync(ct);
        db.Equipment.AddRange(EquipmentDefaults.All.Where(x => !equipmentIds.Contains(x.Id)));
        await db.SaveChangesAsync(ct);
        await transaction.CommitAsync(ct);
    }
}
