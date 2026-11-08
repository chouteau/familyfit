using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Web.Services;

public static class DemoSeeder
{
    public static async Task SeedAsync(FamilyDb db, TimeProvider clock)
    {
        if (await db.Members.AnyAsync()) return;
        var today = FamilyCalendar.Today(clock); var monday = FamilyCalendar.Monday(today); var now = clock.GetUtcNow();
        var names = new[] { "Marc", "Léa", "Hugo", "Emma", "Noé" };
        var members = names.Select((name, i) => new Member { Nickname = name, Age = new[] { 58, 30, 27, 24, 22 }[i], WeightKg = new[] { 68d, 61, 76, 58, 72 }[i], Level = i == 0 ? "Avancé" : "Intermédiaire", IsAdmin = i == 0, AvatarColor = new[] { "green", "orange", "blue", "pink", "yellow" }[i], Goal = "Se renforcer et garder le rythme", SessionMinutes = 30 + i * 5, LastSyncUtc = now.AddMinutes(-i * 3) }).ToList();
        db.Members.AddRange(members); await db.SaveChangesAsync();
        var bodyExercises = await db.Exercises.Where(x => x.EquipmentId == "BODYWEIGHT").Take(5).ToListAsync();
        for (var i = 0; i < members.Count; i++)
        {
            var member = members[i];
            var location = new Location { MemberId = member.Id, Name = i is 1 or 2 ? "Basic-Fit" : "Salle maison", EquipmentIds = "BODYWEIGHT,MAT,DUMBBELLS,ELLIPTICAL,LAT_PULLDOWN,CHEST_PRESS,SEATED_ROW" };
            db.Locations.Add(location);
            db.Weights.Add(new WeightEntry { MemberId = member.Id, WeightKg = member.WeightKg, RecordedAtUtc = now });
            var program = new WorkoutProgram { MemberId = member.Id, Title = "Ma semaine en mouvement", WeekStart = monday, State = "published", Version = 1, CreatedAtUtc = now, PublishedAtUtc = now, ApprovedBy = members[0].Id };
            for (var offset = 0; offset < 7; offset++)
            {
                var date = monday.AddDays(offset);
                var rest = date == today ? i == 4 : offset is 1 or 4 or 6;
                var day = new ProgramDay { ProgramId = program.Id, Date = date, Title = rest ? "Récupération" : new[] { "Renforcement", "Cardio & gainage", "Full body", "Mobilité", "Équilibre" }[(i + offset) % 5], IsRest = rest, LocationId = rest ? null : location.Id, DurationMinutes = rest ? 0 : member.SessionMinutes, Exercises = rest ? [] : bodyExercises.Take(4).Select(e => new Prescription(e.Id, 3, e.TargetType == "time" ? 40 : 12, 20)).ToList() };
                program.Days.Add(day);
                if (!rest && (date < today || (date == today && i < 3)))
                {
                    var partial = date < today && (offset + i) % 4 == 0;
                    var performance = day.Exercises.Select((p, index) => new ExercisePerformance(p.ExerciseId, partial && index == 3 ? 0 : p.Sets, Skipped: partial && index == 3)).ToList();
                    db.Results.Add(new WorkoutResult { Id = Guid.NewGuid(), MemberId = member.Id, ProgramDayId = day.Id, StartedAtUtc = new DateTimeOffset(date.ToDateTime(new TimeOnly(7, 0)), TimeSpan.Zero), FinishedAtUtc = new DateTimeOffset(date.ToDateTime(new TimeOnly(7, 35)), TimeSpan.Zero), ReceivedAtUtc = now, Status = partial ? "partial" : "completed", PerformanceJson = JsonDefaults.Write(performance), SnapshotJson = "{}", PayloadHash = "demo" });
                }
            }
            db.Programs.Add(program);
        }
        // This token is deliberately not usable: it exists only as a revocable browser-session parent in the isolated demonstration database.
        db.Devices.Add(new Device { MemberId = members[0].Id, Name = "Téléphone de démonstration", TokenHash = AuthService.Hash(AuthService.Secret()), CreatedAtUtc = now });
        await db.SaveChangesAsync();
    }
}
