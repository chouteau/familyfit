using ChannelMediator;
using FamilyFit.Contracts;
using FamilyFit.Contracts.Models;
using FamilyFit.Contracts.Models.Members;
using FamilyFit.Core.Data;
using FamilyFit.Core.Services;
using FamilyFit.Core.Validators;
using Microsoft.EntityFrameworkCore;

namespace FamilyFit.Core.Handlers.Members;

public sealed class SaveMemberRequestHandler(IDbContextFactory<FamilyDb> factory, AuthService auth, IMediator mediator, TimeProvider clock)
    : AuthorizedHandler<SaveMemberRequest, Member>(auth, mediator)
{
    protected override async Task<Outcome<Member>> Execute(SaveMemberRequest request, Actor actor, CancellationToken ct)
    {
        if (request.Input == null) return Outcome<Member>.Fail("Profil absent.");
        var error = MemberValidator.Validate(request.Input);
        if (error != null) return Outcome<Member>.Fail(error);
        if (request.Id != null && !actor.IsAdmin && request.Id != actor.MemberId) return Outcome<Member>.Fail("Tu ne peux modifier que ton propre profil.", 403);
        await using var db = await factory.CreateDbContextAsync(ct);
        var member = request.Id == null ? new Member() : await db.Members.SingleOrDefaultAsync(x => x.Id == request.Id, ct);
        if (member == null) return Outcome<Member>.Fail("Membre introuvable.", 404);
        if (request.Id != null && member.Revision != request.Input.Revision) return Outcome<Member>.Fail("Le profil a changé. Recharge les informations.", 409);
        var previousWeight = member.WeightKg;
        var input = request.Input;
        member.Nickname = input.Nickname.Trim(); member.Age = input.Age; member.WeightKg = input.WeightKg;
        member.Goal = input.Goal.Trim(); member.Level = input.Level; member.SessionMinutes = input.SessionMinutes;
        member.TrainingDays = input.TrainingDays; member.ExcludedExerciseIds = input.ExcludedExerciseIds.Trim(); member.Revision++;
        if (request.Id == null) { member.AvatarColor = new[] { "green", "orange", "blue", "pink", "yellow" }[(await db.Members.CountAsync(ct)) % 5]; db.Members.Add(member); }
        if (request.Id == null || previousWeight != member.WeightKg) db.Weights.Add(new WeightEntry { MemberId = member.Id, WeightKg = member.WeightKg, RecordedAtUtc = clock.GetUtcNow() });
        await db.SaveChangesAsync(ct);
        return Outcome<Member>.Ok(member);
    }
}
