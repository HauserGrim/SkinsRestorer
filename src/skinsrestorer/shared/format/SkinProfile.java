/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package skinsrestorer.shared.format;

import java.lang.reflect.Type;
import java.util.UUID;

import skinsrestorer.libs.com.google.gson.JsonDeserializationContext;
import skinsrestorer.libs.com.google.gson.JsonDeserializer;
import skinsrestorer.libs.com.google.gson.JsonElement;
import skinsrestorer.libs.com.google.gson.JsonObject;
import skinsrestorer.libs.com.google.gson.JsonParseException;
import skinsrestorer.libs.com.google.gson.JsonPrimitive;
import skinsrestorer.libs.com.google.gson.JsonSerializationContext;
import skinsrestorer.libs.com.google.gson.JsonSerializer;
import skinsrestorer.shared.utils.SkinFetchUtils;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException.Reason;
import skinsrestorer.shared.utils.UUIDUtil;

public class SkinProfile implements Cloneable {

	long timestamp;
	boolean isForced;
	Profile profile;
	SkinProperty skin;

	public SkinProfile(Profile profile, SkinProperty skinData, long creationTime, boolean isForced) {
		this.profile = profile;
		this.skin = skinData;
		this.timestamp = creationTime;
		this.isForced = isForced;
	}

	public String getName() {
		return profile.getName();
	}

	public void attemptUpdate(String playername) throws SkinFetchFailedException {
		if (!shouldUpdate()) {
			return;
		}
		try {
			SkinProfile newskinprofile = SkinFetchUtils.fetchSkinProfile(playername, profile != null ? getUUID() : null);
			this.timestamp = System.currentTimeMillis();
			this.profile = newskinprofile.profile;
			this.skin = newskinprofile.skin;
		} catch (SkinFetchFailedException e) {
			if (e.getReason() == Reason.NO_PREMIUM_PLAYER || e.getReason() == Reason.NO_SKIN_DATA) {
				this.timestamp = System.currentTimeMillis();
				return;
			}
			throw e;
		}
	}

	private UUID getUUID() {
		return UUIDUtil.fromDashlessString(profile.getId());
	}

	private boolean shouldUpdate() {
		return (System.currentTimeMillis() - timestamp) >= (2 * 60 * 60 * 1000) && !isForced;
	}

	public void applySkin(ApplyFunction applyfunction) {
		if (skin != null) {
			applyfunction.applySkin(skin);
		}
	}

	public SkinProfile cloneAsForced() {
		return new SkinProfile(profile.clone(), skin, timestamp, true);
	}

	@Override
	public SkinProfile clone() {
		return new SkinProfile(profile.clone(), skin, timestamp, isForced);
	}


	public static class GsonTypeAdapter implements JsonSerializer<SkinProfile>, JsonDeserializer<SkinProfile> {

		@Override
		public SkinProfile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject profile = json.getAsJsonObject().get("profile").getAsJsonObject();
			JsonObject skin = json.getAsJsonObject().get("skin").getAsJsonObject();
			return new SkinProfile(
				new Profile(profile.get("uuid").getAsString(), profile.get("name").getAsString()),
				new SkinProperty(skin.get("name").getAsString(), skin.get("value").getAsString(), skin.get("signature").getAsString()),
				json.getAsJsonObject().get("created").getAsLong(),
				json.getAsJsonObject().get("forced").getAsBoolean()
			);
		}

		@Override
		public JsonElement serialize(SkinProfile src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject object = new JsonObject();
			object.add("created", new JsonPrimitive(src.timestamp));
			object.add("forced", new JsonPrimitive(src.isForced));
			JsonObject profile = new JsonObject();
			profile.add("uuid", new JsonPrimitive(src.profile.getId()));
			profile.add("name", new JsonPrimitive(src.profile.getName()));
			object.add("profile", profile);
			JsonObject skin = new JsonObject();
			skin.add("name", new JsonPrimitive(src.skin.getName()));
			skin.add("value", new JsonPrimitive(src.skin.getValue()));
			skin.add("signature", new JsonPrimitive(src.skin.getSignature()));
			object.add("skin", skin);
			return object;
		}

	}

	public static interface ApplyFunction {

		public void applySkin(SkinProperty property);

	}

}
